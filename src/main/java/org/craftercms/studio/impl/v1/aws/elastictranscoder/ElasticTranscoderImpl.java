/*
 * Copyright (C) 2007-2018 Crafter Software Corporation. All rights reserved.
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.craftercms.studio.impl.v1.aws.elastictranscoder;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.elastictranscoder.AmazonElasticTranscoder;
import com.amazonaws.services.elastictranscoder.AmazonElasticTranscoderClientBuilder;
import com.amazonaws.services.elastictranscoder.model.*;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.studio.api.v1.aws.transcoding.Transcoder;
import org.craftercms.studio.api.v1.aws.transcoding.TranscoderJob;
import org.craftercms.studio.api.v1.aws.transcoding.TranscoderOutput;
import org.craftercms.studio.api.v1.aws.transcoding.TranscoderProfile;
import org.craftercms.studio.api.v1.aws.transcoding.aws.AbstractAWSTranscoder;
import org.craftercms.studio.api.v1.exception.AwsException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link Transcoder}. Just as indicated by the interface, the video file is first uploaded to the
 * S3 input bucket of the AWS Elastic Transcoder pipeline, but before uploading, a unique bucket key is generated so that there's no
 * issue when a file with the same filename is uploaded several times. In this way all video file versions (original or transcoded) are
 * kept at all times and there's no downtime when a file is transcoded again.
 *
 * @author avasquez
 */
public class ElasticTranscoderImpl extends AbstractAWSTranscoder {


    @Override
    public TranscoderJob startJob(String filename, InputStream content, TranscoderProfile profile) throws AwsException {
        try {
            AmazonS3 s3Client = getS3Client(profile);
            AmazonElasticTranscoder transcoderClient = getTranscoderClient(profile);
            Pipeline pipeline = getPipeline(profile.getPipelineId(), transcoderClient);
            String baseKey = FilenameUtils.removeExtension(filename) + "/" + UUID.randomUUID().toString();
            String inputKey = baseKey + "." + FilenameUtils.getExtension(filename);

            uploadInput(inputKey, filename, content, pipeline, s3Client);

            CreateJobResult jobResult = createJob(inputKey, baseKey, profile, transcoderClient);

            return createResult(baseKey, jobResult, pipeline);
        } catch (Exception e) {
            throw new AwsException("Error while attempting to start an AWS Elastic Transcoder job for file " + filename, e);
        }
    }

    protected Pipeline getPipeline(String pipelineId, AmazonElasticTranscoder client) {
        ReadPipelineRequest readPipelineRequest = new ReadPipelineRequest();
        readPipelineRequest.setId(pipelineId);

        ReadPipelineResult result = client.readPipeline(readPipelineRequest);

        return result.getPipeline();
    }


    protected CreateJobResult createJob(String inputKey, String baseKey, TranscoderProfile profile,
                                        AmazonElasticTranscoder transcoderClient) {
        CreateJobRequest jobRequest = getCreateJobRequest(inputKey, baseKey, profile);
        CreateJobResult jobResult = transcoderClient.createJob(jobRequest);

        return jobResult;
    }

    protected TranscoderJob createResult(String baseKey, CreateJobResult jobResult, Pipeline pipeline) {
        TranscoderJob job = new TranscoderJob();
        job.setId(jobResult.getJob().getId());
        job.setOutputBucket(pipeline.getOutputBucket());
        job.setBaseKey(baseKey);

        return job;
    }

    protected AmazonElasticTranscoder getTranscoderClient(TranscoderProfile profile) {
        return AmazonElasticTranscoderClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(profile.getCredentials()))
            .withRegion(profile.getRegion())
            .build();
    }

    protected CreateJobRequest getCreateJobRequest(String inputKey, String baseKey, TranscoderProfile profile) {
        JobInput jobInput = new JobInput();
        jobInput.setKey(inputKey);

        List<CreateJobOutput> jobOutputs = new ArrayList<>(profile.getOutputs().size());

        for (TranscoderOutput output : profile.getOutputs()) {
            jobOutputs.add(getCreateJobOutput(baseKey, output));
        }

        CreateJobRequest jobRequest = new CreateJobRequest();
        jobRequest.setPipelineId(profile.getPipelineId());
        jobRequest.setInput(jobInput);
        jobRequest.setOutputs(jobOutputs);

        return jobRequest;
    }

    protected CreateJobOutput getCreateJobOutput(String baseKey, TranscoderOutput output) {
        CreateJobOutput jobOutput = new CreateJobOutput();
        jobOutput.setPresetId(output.getPresetId());
        jobOutput.setKey(baseKey + output.getOutputKeySuffix());

        if (StringUtils.isNotEmpty(output.getThumbnailSuffixFormat())) {
            jobOutput.setThumbnailPattern(baseKey + output.getThumbnailSuffixFormat());
        }

        return jobOutput;
    }

}


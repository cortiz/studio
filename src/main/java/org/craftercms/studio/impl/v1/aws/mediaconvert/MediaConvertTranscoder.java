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

package org.craftercms.studio.impl.v1.aws.mediaconvert;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.mediaconvert.AWSMediaConvertAsync;
import com.amazonaws.services.mediaconvert.AWSMediaConvertAsyncClientBuilder;
import com.amazonaws.services.mediaconvert.AWSMediaConvertClient;
import com.amazonaws.services.mediaconvert.model.*;
import org.craftercms.studio.api.v1.aws.transcoding.TranscoderJob;
import org.craftercms.studio.api.v1.aws.transcoding.TranscoderProfile;
import org.craftercms.studio.api.v1.aws.transcoding.aws.AbstractAWSTranscoder;
import org.craftercms.studio.api.v1.exception.TranscodingException;

import java.io.InputStream;

/**
 * Default implementation of {@link org.craftercms.studio.api.v1.aws.transcoding.Transcoder}. Just as indicated by the interface, the video file is first uploaded to the
 * S3 input bucket of the AWS Media Converter pipeline, but before uploading, a unique bucket key is generated so that there's no
 * issue when a file with the same filename is uploaded several times. In this way all video file versions (original or transcoded) are
 * kept at all times and there's no downtime when a file is transcoded again.
 *
 * @author Carlos Ortiz
 */
public class MediaConvertTranscoder extends AbstractAWSTranscoder {


	/**
	 * @inheritDoc
	 */
	@Override
	public TranscoderJob startJob(final String filename, final InputStream content,
								  final TranscoderProfile profile)
			throws TranscodingException {
		AWSMediaConvertAsync mediaConvertClient = buildAwsClient(profile);
		final GetJobTemplateResult jtmp = mediaConvertClient.getJobTemplate(jobTemplateRequestFor(profile.getPipelineId()));
		final JobTemplate jobtemplate = jtmp.getJobTemplate();
		final JobRequest request = createJobRequest(jobtemplate, "s3://", mediaConvertClient);
		return null;
	}

	private JobRequest createJobRequest(final JobTemplate jobtemplate, final String path, final AWSMediaConvertAsync mediaConvertClient) {
		final CreateJobRequest jobRequest = new CreateJobRequest();
		jobRequest.withJobTemplate(jobtemplate.getName());
		jobRequest.setSettings(new JobSettings().withInputs(new Input().withFileInput(path)));
		return jobRequest
	}

	private AWSMediaConvertAsync buildAwsClient(final TranscoderProfile profile) {
		return AWSMediaConvertAsyncClientBuilder.standard().withCredentials(
				new AWSStaticCredentialsProvider(profile.getCredentials())).withRegion(profile.getRegion())
				.build();
	}

	/**
	 *  Gets the given template
	 * @param jobTemplateName
	 * @return A JobTemplate
	 */
	private GetJobTemplateRequest jobTemplateRequestFor(final String jobTemplateName) {
		final GetJobTemplateRequest gjtr = new GetJobTemplateRequest();
		gjtr.withName(jobTemplateName);
		return gjtr;
	}


}

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

package org.craftercms.studio.api.v1.aws.transcoding.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.elastictranscoder.model.Pipeline;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.craftercms.studio.api.v1.aws.transcoding.Transcoder;
import org.craftercms.studio.api.v1.aws.transcoding.TranscoderProfile;
import org.craftercms.studio.api.v1.exception.AwsException;
import org.craftercms.studio.impl.v1.service.aws.AwsUtils;

import java.io.InputStream;

/**
 *
 */
public abstract class AbstractAWSTranscoder implements Transcoder {

    private int partSize;

    public AbstractAWSTranscoder() {
        this(AwsUtils.MIN_PART_SIZE);
    }

    public AbstractAWSTranscoder(int partSize) {
        this.partSize = partSize;
    }

    protected void uploadInput(String inputKey, String filename, InputStream content, Pipeline pipeline,
                               AmazonS3 s3Client) throws AwsException {
        String inputBucket = pipeline.getInputBucket();
        AwsUtils.uploadStream(inputBucket, inputKey, s3Client, partSize, filename, content);
    }

    protected AmazonS3 getS3Client(TranscoderProfile profile) {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(profile.getCredentials()))
                .withRegion(profile.getRegion())
                .build();
    }
}

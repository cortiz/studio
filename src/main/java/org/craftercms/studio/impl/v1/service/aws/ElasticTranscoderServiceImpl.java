package org.craftercms.studio.impl.v1.service.aws;

import java.io.InputStream;

import org.craftercms.commons.validation.annotations.param.ValidateParams;
import org.craftercms.commons.validation.annotations.param.ValidateStringParam;
import org.craftercms.studio.api.v1.aws.AwsProfileReader;
import org.craftercms.studio.api.v1.aws.transcoding.Transcoder;
import org.craftercms.studio.api.v1.aws.transcoding.TranscoderJob;
import org.craftercms.studio.api.v1.aws.transcoding.TranscoderProfile;
import org.craftercms.studio.api.v1.exception.AwsException;
import org.craftercms.studio.api.v1.service.aws.AbstractAwsService;
import org.craftercms.studio.api.v1.service.aws.ElasticTranscoderService;
import org.springframework.beans.factory.annotation.Required;

/**
 * Default implementation of {@link ElasticTranscoderService}. It uses a {@link AwsProfileReader} to get the specified transcoding
 * profile and a {@link Transcoder} instance to start the transcoding job.
 *
 * @author avasquez
 */
public class ElasticTranscoderServiceImpl extends AbstractAwsService<TranscoderProfile> implements ElasticTranscoderService {

    private Transcoder transcoder;

    @Required
    public void setTranscoder(Transcoder transcoder) {
        this.transcoder = transcoder;
    }

    @Override
    @ValidateParams
    public TranscoderJob transcodeFile(@ValidateStringParam(name = "site") String site,
                                       @ValidateStringParam(name = "profileId") String profileId,
                                       @ValidateStringParam(name = "filename") String filename,
                                       InputStream content) throws AwsException {
        TranscoderProfile profile = getProfile(site, profileId);
        TranscoderJob job = transcoder.startJob(filename, content, profile);

        return job;
    }

}

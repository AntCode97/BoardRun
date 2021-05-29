package boardrun.yunjun.service;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import lombok.Getter;
import lombok.Setter;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Getter @Setter
public class ThumbnailService {
    private Logger log = LoggerFactory.getLogger(getClass().getName());
    private static final String IMAGE_PNG_FORMAT = "png";

    /**
     *
     *
     * @param source mp4 file.
     * @param thumbnail
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public File getThumbnail(File source, File thumbnail) throws IOException, JCodecException {

        log.debug("extracting thumbnail from video");
        int frameNumber = 0;

        try {
            Picture picture = FrameGrab.getFrameFromFile(source, frameNumber);

            BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
            ImageIO.write(bufferedImage, IMAGE_PNG_FORMAT, thumbnail);
        }catch (Exception e){
            System.out.println(e);
        }

        return thumbnail;
    }
}

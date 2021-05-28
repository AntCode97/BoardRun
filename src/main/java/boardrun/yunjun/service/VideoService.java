package boardrun.yunjun.service;

import boardrun.yunjun.domain.Video;
import boardrun.yunjun.domain.VideoImg;
import boardrun.yunjun.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Transactional
public class VideoService {
    private final VideoRepository videoRepository;

    public void makeVideoImg(Long videoId) {
        Video video = videoRepository.findOne(videoId);
        VideoImg videoImg = new VideoImg();
        videoImg.setVideo(video);
    }

    //회원 가입
    public Long upload(Video video) {
        File file = new File("./upload-dir/"+video.getFileName());
        String formatted = "";
        BasicFileAttributes attrs = null;
        FileTime time= null;
        try {
            attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            time = attrs.creationTime();
            String pattern = "yyyy-MM-dd";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            formatted = simpleDateFormat.format(new Date(time.toMillis()));
            video.setCreatedAt(new Date(time.toMillis()));
            System.out.println("생성날짜 : " + new Date(time.toMillis()));
            System.out.println("형식지정표현 생성날짜 : " + formatted);
        } catch (IOException e) {
            e.printStackTrace();
        }


        videoRepository.save(video);
        return video.getId(); //em에서 persist를 실행하면 영속성으로 엔티티를 저장한다.=> 엔티티를 영구 저장한다
    }


}

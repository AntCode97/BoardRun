package boardrun.yunjun.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import boardrun.yunjun.domain.Video;
import boardrun.yunjun.domain.VideoImg;
import boardrun.yunjun.service.DetectionService;
import boardrun.yunjun.service.ThumbnailService;
import boardrun.yunjun.service.VideoService;
import boardrun.yunjun.storage.StorageFileNotFoundException;
import boardrun.yunjun.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final DetectionService detectionService;
    private final StorageService storageService;
    //private ThumbnailService thumbnailService = new ThumbnailService();


//    @Autowired
//    public FileUploadController(StorageService storageService) {
//        this.storageService = storageService;
//    }

    @GetMapping("/uploadvideo")
    public String listUploadedFiles(Model model) throws IOException {

        model.addAttribute("files", storageService.loadAll().map(
                path -> MvcUriComponentsBuilder.fromMethodName(VideoController.class,
                        "serveFile", path.getFileName().toString()).build().toUri().toString())
                .collect(Collectors.toList()));
        try {
            List<Video> videos = videoService.findVideos();
            Video video = videos.get(videos.size()-1);
            File videoFile = new File(video.getFilePath());
            model.addAttribute("videoFile", video.getFilePath());
        }catch (Exception e) {
            System.out.println(e);
        }

//        try{
//            List<Video> videos = videoService.findVideos();
//            Video video = videos.get(videos.size()-1);
//            File videoFile = new File(video.getFilePath());
//            File thumbnailFile = new File("./thumbnail.png") ;
//            thumbnailFile = thumbnailService.getThumbnail(videoFile, thumbnailFile);
//            model.addAttribute("thumnail", thumbnailFile);
//        } catch (Exception e){
//            System.out.println(e);
//        };

        return "uploadvideo";
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/uploadvideo")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {

        storageService.store(file);
        Video video = new Video();

        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded " + file.getOriginalFilename() + "!");
        video.setFileName(file.getOriginalFilename());
        videoService.upload(video);
        return "redirect:/uploadvideo";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }



//    @PostMapping("/videoList")
//    public String create(VideoForm form){
//
//
//
//        return "redirect:/";
//    }

    @GetMapping("/videoList")
    public String video(Model model) {

//        File dir = new File("./detections");
//        File files[] = dir.listFiles();
//        List<String> files_names = new ArrayList<String>();
//        for (File file: files){
//            files_names.add("/detections/" + file.getName());
//        }
        List<VideoImg> videoImgs = videoService.findVideoImgs();
        videoImgs.get(0).getFilePath();
        model.addAttribute("detectionList", videoImgs);

        return "videoList";
    }

    @GetMapping("/detection")
    public String detection(){
        List<Video> videos = videoService.findVideos();
        Video video = videos.get(videos.size()-1);
        detectionService.detect_violation(video.getFileName());
        VideoImg videoImg = new VideoImg();
        videoImg.setVideo(video);

        String[] imgName = video.getFileName().split("\\.");
        videoImg.setFilePath("./detections/" + imgName[0] +".png");
        videoImg.setCreatedAt(video.getCreatedAt());
        videoService.imgSave(videoImg);
        return  "redirect:/uploadvideo";
    }

}
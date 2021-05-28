package boardrun.yunjun.controller;

import boardrun.yunjun.domain.Video;
import boardrun.yunjun.service.VideoService;
import boardrun.yunjun.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class VideoController2 {

    private final VideoService videoService;
    private final StorageService storageService;

//    @GetMapping("/uploadvideo")
//    public String uploadvideo(Model model) {
//        //model에 데이터를 실어서 View에 넘길 수 있다.
//        model.addAttribute("videoForm", new VideoForm());
//        return "uploadvideo"; //리턴은 화면 이름이다. thymeleaf가 알아서 hello라는 이름을 가진 html파일과 연결해준다.
//    }
//
//    @PostMapping("/")
//    public String handleFileUpload(@RequestParam("file") MultipartFile file,
//                                   RedirectAttributes redirectAttributes) {
//
//        storageService.store(file);
//        redirectAttributes.addFlashAttribute("message",
//                "You successfully uploaded " + file.getOriginalFilename() + "!");
//
//        return "redirect:/";
//    }
//
//
//    @PostMapping("/videoList")
//    public String create(VideoForm form){
//        Video video = new Video();
//        video.setFileUrl(form.getFileUrl());
//
//        videoService.upload(video);
//        return "redirect:/";
//    }
//
//    @GetMapping("/videoList")
//    public String video(Model model) {
//        //model에 데이터를 실어서 View에 넘길 수 있다.
//        model.addAttribute("data", "hello!!");
//        return "videoList"; //리턴은 화면 이름이다. thymeleaf가 알아서 hello라는 이름을 가진 html파일과 연결해준다.
//    }
}

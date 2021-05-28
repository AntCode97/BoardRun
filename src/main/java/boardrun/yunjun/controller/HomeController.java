package boardrun.yunjun.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        log.info("home controller");
        //model에 데이터를 실어서 View에 넘길 수 있다.
        model.addAttribute("data", "hello!!");
        return "main"; //리턴은 화면 이름이다. thymeleaf가 알아서 hello라는 이름을 가진 html파일과 연결해준다.
    }


}


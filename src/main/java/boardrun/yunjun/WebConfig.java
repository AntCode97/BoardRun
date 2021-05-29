package boardrun.yunjun;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import lombok.RequiredArgsConstructor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadImagesPath;

    public WebConfig(@Value("${custom.path.upload-images}") String uploadImagesPath){
        this.uploadImagesPath = uploadImagesPath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry){
//        registry.addResourceHandler("videoList.html").addResourceLocations("classpath:/static/");
        //registry.addResourceHandler("/upload-dir").addResourceLocations();
        registry.addResourceHandler("/**").addResourceLocations("file:///C:/Users/dnslab_wolf/IdeaProjects/boardrun/upload-dir", "file:src/main/resources/static/img","file:///C:/Users/dnslab_wolf/IdeaProjects/boardrun/detections");
//        List<String> imageFolders = Arrays.asList("detections");
//        for(String imageFolder : imageFolders) {
//            registry.addResourceHandler("/static/img/" + imageFolder+"/**")
//                    .addResourceLocations("file:///" + uploadImagesPath + imageFolder +'/').setCachePeriod(3600).resourceChain(true)
//                    .addResolver(new PathResourceResolver());
//        }
    }


}


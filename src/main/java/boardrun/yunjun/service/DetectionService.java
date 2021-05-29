package boardrun.yunjun.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.concurrent.Executors;

@Service
@Getter @Setter
public class DetectionService {


    public void detect_violation(String filePath){
        try {
            // Linux의 경우는 /bin/bash
            // Process process = Runtime.getRuntime().exec("/bin/bash");
            Process process = Runtime.getRuntime().exec("cmd");
            // Process의 각 stream을 받는다.
            // process의 입력 stream
            OutputStream stdin = process.getOutputStream();
            // process의 에러 stream
            InputStream stderr = process.getErrorStream();
            // process의 출력 stream
            InputStream stdout = process.getInputStream();
            // 쓰레드 풀을 이용해서 3개의 stream을 대기시킨다.
            // 출력 stream을 BufferedReader로 받아서 라인 변경이 있을 경우 console 화면에 출력시킨다.
            Executors.newCachedThreadPool().execute(() -> {
                // 문자 깨짐이 발생할 경우 InputStreamReader(stdout)에 인코딩 타입을 넣는다. ex)
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stdout,"euc-kr"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
            // 에러 stream을 BufferedReader로 받아서 에러가 발생할 경우 console 화면에 출력시킨다.
            Executors.newCachedThreadPool().execute(() -> {
                // 문자 깨짐이 발생할 경우 InputStreamReader(stdout)에 인코딩 타입을 넣는다. ex)
                // InputStreamReader(stdout, "euc-kr")
                // try (BufferedReader reader = new BufferedReader(new InputStreamReader(stderr,
                // "euc-kr"))) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stderr, "euc-kr"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("err " + line);
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
            // 입력 stream을 BufferedWriter로 받아서 콘솔로부터 받은 입력을 Process 클래스로 실행시킨다.
            Executors.newCachedThreadPool().execute(() -> {
                // Scanner 클래스는 콘솔로 부터 입력을 받기 위한 클래스 입니다.
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin))) {
                    // D:
                    // cd capstone/BoardRun
                    // python detect_violation.py --video ./data/video/side_9.mp4

                    // D 드라이브로 이동
                    String input = "D:\n";
                    writer.write(input);

                    // detect_violation.py 파일이 있는 폴더로 이동
                    input = "cd capstone/BoardRun\n";
                    writer.write(input);

                    // detect_violation.py 실행
                    input = "python detect_violation.py --img "+"C:/Users/dnslab_wolf/IdeaProjects/boardrun/detections/"+filePath.split("\\.")[0]+".png " + "--dont_show True --video "+ "C:/Users/dnslab_wolf/IdeaProjects/boardrun/upload-dir/"+ filePath +"\n";
                    writer.write(input);

                    writer.flush();

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}

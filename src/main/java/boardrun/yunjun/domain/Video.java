package boardrun.yunjun.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
public class Video {

    @Id
    @GeneratedValue //@Id는 해당 테이블의 PK필드를 나타낸다. //GeneratedValue는 PK의 생성규칙을 나타내고, 기본값은 AUTO로 MySQL의 auto_increment와 같이 자동 증가하는 정수형 값이 된다.
    @Column(name = "video_id")
    private Long id;

    private String fileName;
    private String filePath;

    private Date createdAt;


    @OneToMany(mappedBy = "video") //누구에 의해서 매핑되는가,
    private List<VideoImg> videoImgs = new ArrayList<>();

    public void setFileName(String fileName) {
        this.fileName = fileName;
        this.filePath="./upload-dir/"+this.getFileName();
    }
}

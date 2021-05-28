package boardrun.yunjun.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "videoimgs")
@Getter @Setter
public class VideoImg {
    @Id
    @GeneratedValue
    @Column(name = "videoimg_id")
    private Long id;

    private String file_url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video; // 주문 회원

    // ==연관관계 메서드==//
    public void setVideo(Video video) {
        this.video = video;
        video.getVideoImgs().add(this);
    }


}

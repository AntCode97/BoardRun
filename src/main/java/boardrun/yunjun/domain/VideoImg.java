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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video; // 주문 회원

}

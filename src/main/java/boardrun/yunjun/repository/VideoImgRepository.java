package boardrun.yunjun.repository;

import boardrun.yunjun.domain.Video;
import boardrun.yunjun.domain.VideoImg;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class VideoImgRepository {

    private final EntityManager em;

    public void save(VideoImg videoImg){
        em.persist(videoImg);
    }

    public VideoImg findOne(Long id) {
        return em.find(VideoImg.class, id);
    }

    public List<VideoImg> findAll(){
        //Video.class는 반환 타입
        List<VideoImg> result = em.createQuery("select m from VideoImg m", VideoImg.class).getResultList();
        return result;
    }

    public List<VideoImg> findById(Long id) {
        return em.createQuery("select m from VideoImg m where m.id = :id", VideoImg.class)
                .setParameter("id", id)
                .getResultList();
    }
}

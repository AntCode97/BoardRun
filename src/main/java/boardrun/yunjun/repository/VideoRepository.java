package boardrun.yunjun.repository;

import boardrun.yunjun.domain.Video;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class VideoRepository {
    //@PersistenceContext //SpringBoot에서는 이걸 Autowired로해도 작동하기 때문에, RequiredArgsConstructor를 쓸 수 있다.
    private final EntityManager em;

    // 직접 선언해서 매니저 팩토리를 주입받을 수 있지만 Em에서 spring이 알아서 해줘서 할 필요가 없다
    //@PersistenceUnit
    //private EntityManagerFactory emf;

    public void save(Video video){
        em.persist(video);
    }

    public Video findOne(Long id) {
        return em.find(Video.class, id);
    }

    public List<Video> findAll(){
        //Video.class는 반환 타입
        List<Video> result = em.createQuery("select m from Video m", Video.class).getResultList();
        return result;
    }

    public List<Video> findByName(Long id) {
        return em.createQuery("select m from Video m where m.id = :id", Video.class)
                .setParameter("id", id)
                .getResultList();
    }
}

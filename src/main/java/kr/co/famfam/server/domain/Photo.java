package kr.co.famfam.server.domain;

import lombok.Data;

import javax.persistence.*;

/**
 * Created by ehay@naver.com on 2018-12-25
 * Blog : http://ehay.tistory.com
 * Github : http://github.com/ehayand
 */

@Data
@Entity
@Table(name = "photo")
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "photo_seq_generator", sequenceName = "photo_seq", allocationSize = 1)
    private int photoIdx;

    private String photoName;

    private int contentIdx;
    private int userIdx;
}

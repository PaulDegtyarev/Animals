package hibernate;


import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Timestamp;

@Entity(name="Animal")
@Table(name = "animal", schema = "animals")
public class Animal implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "animaltypes")
    public BigInteger[] animaltypes;

    @Column(name = "weight")
    public Float weight;

    @Column(name = "length")
    public Float length;

    @Column(name = "height")
    public Float height;

    @Column(name = "gender")
    public String gender;

    @Column(name = "lifestatus")
    public String lifestatus;

    @Column(name = "chippingdatetime")
    public Timestamp chippingdatetime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", foreignKey = @ForeignKey(name = "chipperid"))
    public Users chipperid;

    @Column(name = "chippinglocationid")
    public Integer chippinglocationid;

    @Column(name = "visitedlocationid")
    public BigInteger[] visitedlocationid;

    @Column(name = "deathdatetime")
    public Timestamp deathdatetime;
}

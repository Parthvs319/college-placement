package helpers.blueprint.models;

import helpers.sql.SqlConfigFactory;
import io.ebean.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@EqualsAndHashCode(callSuper = false)
@MappedSuperclass
public class BaseModel extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "updated_at")
    private Timestamp updatedAt = new Timestamp(System.currentTimeMillis());;

    @Column(name = "deleted")
    private boolean deleted = false;

    public void save() {
        try {
            if (getCreatedAt() == null || getId() == null)
                SqlConfigFactory.save(this);
            else
                SqlConfigFactory.update(this);
        } catch (Exception e) {
            SqlConfigFactory.save(this);
        }
    }

    public void update() {
        SqlConfigFactory.update(this);
    }

}

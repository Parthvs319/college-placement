package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "states")
public class States extends BaseModel {

    @Column(nullable = false, unique = true)
    public String name;

    @Column(nullable = false, unique = true, length = 5)
    public String code;  // e.g. "MP", "DL", "MH"

}

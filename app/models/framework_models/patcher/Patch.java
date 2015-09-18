package models.framework_models.patcher;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import com.avaje.ebean.Model;

import models.framework_models.parent.IModelConstants;

/**
 * A class which stores the release number of the application.
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class Patch extends Model {
    public static Finder<Long, Patch> find = new Finder<Long, Patch>(Patch.class);

    @Id
    public Long id;

    @Column(length = IModelConstants.SMALL_STRING)
    public String apprelease;

    public Date runDate;

    public Patch() {
    }
}

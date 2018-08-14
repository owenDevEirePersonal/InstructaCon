package com.deveire.dev.instructacon.remastered.TroubleTicketSystem;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by owenryan on 14/08/2018.
 */

public class TroubleAlert
{
    private TroubleTask task;
    private TroubleEmployee assignedEmployee;
    private TroubleEmployee assignedSupervisor;
    private TroubleEmployee raisedBy;
    private Date raisedAt;
    private Date completedAt;
    private Date reviewedAt;
    private String location;

    public TroubleAlert(TroubleTask task, TroubleEmployee raisedBy, Date raisedAt, String location)
    {
        this.task = task;
        this.raisedBy = raisedBy;
        this.raisedAt = raisedAt;
        this.assignedEmployee = null;
        this.assignedSupervisor = null;
        this.completedAt = null;
        this.reviewedAt = null;
        this.location = location;
    }

    public TroubleAlert(TroubleTask task, TroubleEmployee raisedBy, String location)
    {
        this.task = task;
        this.raisedBy = raisedBy;
        Calendar cal = Calendar.getInstance();
        this.raisedAt = cal.getTime();
        this.assignedEmployee = null;
        this.assignedSupervisor = null;
        this.completedAt = null;
        this.reviewedAt = null;
        this.location = location;
    }

    public TroubleAlert(TroubleTask task, TroubleEmployee assignedEmployee, TroubleEmployee assignedSupervisor, TroubleEmployee raisedBy, Date raisedAt, String location)
    {
        this.task = task;
        this.raisedBy = raisedBy;
        this.raisedAt = raisedAt;
        this.assignedEmployee = assignedEmployee;
        this.assignedSupervisor = assignedSupervisor;
        this.completedAt = null;
        this.reviewedAt = null;
        this.location = location;
    }

    public TroubleAlert(TroubleTask task, TroubleEmployee assignedEmployee, TroubleEmployee assignedSupervisor, TroubleEmployee raisedBy, String location)
    {
        this.task = task;
        this.raisedBy = raisedBy;
        Calendar cal = Calendar.getInstance();
        this.raisedAt = cal.getTime();
        this.assignedEmployee = assignedEmployee;
        this.assignedSupervisor = assignedSupervisor;
        this.completedAt = null;
        this.reviewedAt = null;
        this.location = location;
    }

    public TroubleAlert(TroubleTask task, TroubleEmployee assignedEmployee, TroubleEmployee assignedSupervisor, TroubleEmployee raisedBy, Date raisedAt, Date completedAt, Date reviewedAt, String location)
    {
        this.task = task;
        this.assignedEmployee = assignedEmployee;
        this.assignedSupervisor = assignedSupervisor;
        this.raisedBy = raisedBy;
        this.raisedAt = raisedAt;
        this.completedAt = completedAt;
        this.reviewedAt = reviewedAt;
        this.location = location;
    }

    public TroubleAlert(TroubleTask task, TroubleEmployee assignedEmployee, TroubleEmployee assignedSupervisor, TroubleEmployee raisedBy, Date completedAt, Date reviewedAt, String location)
    {
        this.task = task;
        this.assignedEmployee = assignedEmployee;
        this.assignedSupervisor = assignedSupervisor;
        this.raisedBy = raisedBy;
        Calendar cal = Calendar.getInstance();
        this.raisedAt = cal.getTime();
        this.completedAt = completedAt;
        this.reviewedAt = reviewedAt;
        this.location = location;
    }

    public TroubleTask getTask()
    {
        return task;
    }

    public void setTask(TroubleTask task)
    {
        this.task = task;
    }

    public TroubleEmployee getAssignedEmployee()
    {
        return assignedEmployee;
    }

    public void setAssignedEmployee(TroubleEmployee assignedEmployee)
    {
        this.assignedEmployee = assignedEmployee;
    }

    public TroubleEmployee getAssignedSupervisor()
    {
        return assignedSupervisor;
    }

    public void setAssignedSupervisor(TroubleEmployee assignedSupervisor)
    {
        this.assignedSupervisor = assignedSupervisor;
    }

    public TroubleEmployee getRaisedBy()
    {
        return raisedBy;
    }

    public void setRaisedBy(TroubleEmployee raisedBy)
    {
        this.raisedBy = raisedBy;
    }

    public Date getRaisedAt()
    {
        return raisedAt;
    }

    public void setRaisedAt(Date raisedAt)
    {
        this.raisedAt = raisedAt;
    }

    public Date getCompletedAt()
    {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt)
    {
        this.completedAt = completedAt;
    }

    public Date getReviewedAt()
    {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt)
    {
        this.reviewedAt = reviewedAt;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }
}

/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-10-19
 */

package com.tp_link.web.eapsimulator.server.dao;

import com.tp_link.web.eapsimulator.server.po.EapLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;

import java.sql.Connection;
import java.util.List;

public class EapLogDao {


    private static final Log logger = LogFactory.getLog(EapLogDao.class);
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<EapLog> findAllLogs() {
        Session session = sessionFactory.getCurrentSession();
        String qStr = "FROM EapLog l";
        Query query = session.createQuery(qStr);
        return (List<EapLog>) query.list();
    }

    public List<EapLog> findLogsByEapId(String eapId) {
        Session session = sessionFactory.getCurrentSession();
        String qStr = "FROM EapLog l WHERE l.eapId = ?";
        Query query = session.createQuery(qStr);
        query.setString(0, eapId);
        return (List<EapLog>) query.list();
    }

    public int saveLog(EapLog eapLog) {
        Session session = sessionFactory.getCurrentSession();
        session.save(eapLog);
        return 1;
    }

    public int saveLog(List<EapLog> eapLogs) {
        Session session = sessionFactory.getCurrentSession();
        for (EapLog log : eapLogs) {
            session.save(log);
        }
        return eapLogs.size();
    }

    public int deleteLogById(String id) {
        Session session = sessionFactory.getCurrentSession();
        String queryString = "DELETE FROM EapLog l WHERE l.id = ?";
        Query q = session.createQuery(queryString);
        q.setString(0, id);
        return q.executeUpdate();
    }

    public int deleteLogByEapId(String eapId) {
        Session session = sessionFactory.getCurrentSession();
        String qStr = "DELETE FROM EapLog  l WHERE l.eapId = ?";
        Query q = session.createQuery(qStr);
        q.setString(0, eapId);
        return q.executeUpdate();
    }

    public PagedResult<EapLog> getLogPage(int limit, int offset) {
        Session session = sessionFactory.getCurrentSession();
        Query countQuery = session.createQuery("select count (*) from EapLog ");
        int count = ((Long) countQuery.uniqueResult()).intValue();
        Query query = session.createQuery("select p from EapLog p order by p.createTime desc ");
        List<EapLog> list;
        if (limit > 100) {
            // Show all.
            list = (List<EapLog>) query.list();
        } else {
            // Show limited.
            list = (List<EapLog>) query.setFirstResult(offset).setMaxResults(limit).list();
        }
        PagedResult<EapLog> result = new PagedResult<>();
        result.setTotal(count);
        result.setLimit(limit);
        result.setOffset(offset);
        result.setRows(list);
        return result;
    }

    public PagedResult<EapLog> getLogPageByDeviceId(int limit, int offset, String deviceId) {
        Session session = sessionFactory.getCurrentSession();
        Query countQuery = session.createQuery("select count (*) from EapLog where eapId = ?")
                .setString(0, deviceId);
        int count = ((Long) countQuery.uniqueResult()).intValue();
        Query query = session.createQuery("select p from EapLog p where p.eapId = ? order by p.createTime desc ").setString(0, deviceId);
        List<EapLog> list;
        if (limit > 100) {
            // Show all.
            list = (List<EapLog>) query.list();
        } else {
            // Show limited.
            list = (List<EapLog>) query.setFirstResult(offset).setMaxResults(limit).list();
        }
        PagedResult<EapLog> result = new PagedResult<>();
        result.setTotal(count);
        result.setLimit(limit);
        result.setOffset(offset);
        result.setRows(list);
        return result;
    }

}

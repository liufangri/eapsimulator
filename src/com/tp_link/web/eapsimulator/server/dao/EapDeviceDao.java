/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-20
 */

package com.tp_link.web.eapsimulator.server.dao;

import com.tp_link.web.eapsimulator.server.po.EapDevice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class EapDeviceDao {
    private static final Log logger = LogFactory.getLog(EapDeviceDao.class);
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<EapDevice> getDeviceList() {
        Session session = sessionFactory.getCurrentSession();
        List<EapDevice> list;
        list = (List<EapDevice>) session.createQuery("SELECT e FROM EapDevice e").list();
        return list;
    }

    public List<EapDevice> getDeviceListByIds(String ids) {
        StringBuffer stringBuffer = new StringBuffer();
        ids = stringBuffer.append('\'').append(ids).append('\'').toString().replaceAll(",", "\',\'");
        Session session = sessionFactory.getCurrentSession();
        Query query = session.createQuery("SELECT e FROM EapDevice e WHERE e.id in (" + ids + ")");
        List<EapDevice> list;
        list = (List<EapDevice>) query.list();
        return list;
    }

    public int deleteDeviceById(String ids) {
        StringBuffer stringBuffer = new StringBuffer();
        ids = stringBuffer.append('\'').append(ids).append('\'').toString().replaceAll(",", "\',\'");
        Session session = sessionFactory.getCurrentSession();
        Query query = session.createQuery("delete from EapDevice e  WHERE e.id in (" + ids + ")");
        int ret = query.executeUpdate();
        return ret;
    }

    public int addDevice(EapDevice device) {
        Session session = sessionFactory.getCurrentSession();
        // Save returns the Primary Key of device
        session.save(device);
        return 1;
    }

    public int addBatchDevice(List<EapDevice> devices) {
        Session session = sessionFactory.getCurrentSession();
        if (devices == null || devices.size() == 0) {
            return -1;
        } else {
            int i = 0;
            for (EapDevice device : devices) {
                session.save(device);
                if (i % 10 == 0) {
                    session.flush();
                }
                i++;
            }
            return 1;
        }

    }

    public int modifyDevice(EapDevice device) {
        Session session = sessionFactory.getCurrentSession();
        // Update returns nothing
        session.update(device);
        return 1;
    }

    public PagedResult<EapDevice> getAllDeviceListPaged(int limit, int offset) {
        Session session = sessionFactory.getCurrentSession();
        String countHql = "select count(*) from EapDevice";
        Query countQuery = session.createQuery(countHql);
        int count = ((Long) countQuery.uniqueResult()).intValue();
        List<EapDevice> list;
        String hql = "from EapDevice s order by s.createTime desc";
        Query query = session.createQuery(hql);
        if (limit > 100) {
            // Show all.
            list = (List<EapDevice>) query.list();
        } else {
            // Show limited.
            list = (List<EapDevice>) query.setFirstResult(offset).setMaxResults(limit).list();
        }
        PagedResult<EapDevice> res = new PagedResult<>();
        res.setRows(list);
        res.setOffset(offset);
        res.setLimit(limit);
        res.setTotal(count);
        return res;
    }
}

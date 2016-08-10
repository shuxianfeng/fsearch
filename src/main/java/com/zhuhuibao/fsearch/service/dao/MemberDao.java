package com.zhuhuibao.fsearch.service.dao;

import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MapHandler;
import com.petkit.base.repository.db.StringPropertyHandler;
import com.zhuhuibao.fsearch.L;
import com.zhuhuibao.fsearch.core.DataSourceManager;
import com.zhuhuibao.fsearch.service.contants.ConstantType;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jianglz
 * @since 16/7/11.
 */
public class MemberDao {


    /**
     *   根据会员ID查询VIP信息
     * @param memberId
     * @return
     */
    public Map<String, Object> findVipMember(String memberId) {
        Map<String,Object> vipMap = new HashMap<>();
        JdbcTemplate template = DataSourceManager.getJdbcTemplate();

        try {
            Object[] params;
            String sql = "select * from t_vip_member_info where member_id =?";

            params = new Object[]{memberId};

            vipMap = template.findOne(sql, params, MapHandler.CASESENSITIVE);

        } catch (Exception e) {
            e.printStackTrace();
            L.error("[t_vip_member_info]查询失败:" + e);
        }
        return vipMap;
    }

    public static void main(String[] args) {
        MemberDao dao = new MemberDao();
        Map<String,Object> map = dao.findVipMember("515");
        System.out.println(map);
    }
}

package com.movision.fsearch.service.dao;

import com.movision.fsearch.L;
import com.movision.fsearch.core.DataSourceManager;
import com.mysql.jdbc.StringUtils;
import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MapHandler;
import com.petkit.base.repository.db.StringPropertyHandler;
import com.petkit.base.utils.FormatUtil;
import com.movision.fsearch.service.contants.ConstantType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jianglz
 * @since 16/7/10.
 */
public class ProjectDao {


    /**
     * 根据项目分类编码   查询  项目分类名称
     *
     * @param category 项目分类编码
     * @return
     */
    public String findCategoryNames(String category) throws Exception {

        JdbcTemplate template = DataSourceManager.getJdbcTemplate();

        String categoryName;
        try {
            Object[] params;
            String sql = "select group_concat(name) as names " +
                    "    from t_dictionary_constant  " +
                    "    where find_in_set(code, ?) and type= ? ";

            params = new Object[]{category, ConstantType.XMXXLB.value};

            categoryName = template.findOne(sql, params, StringPropertyHandler.getInstance());

        } catch (Exception e) {
            L.error("[t_dictionary_constant]查询失败:" + e);
            throw e;
        }
        return categoryName == null ? "" : categoryName;
    }


    /**
     * 组装新的address
     *
     * @param province provinceCode
     * @param cityName cityName
     * @param area     areaCode
     * @param address  address
     * @return
     */
    public String findAddress(String province, String cityName, String area, String address) throws Exception {
        String provinceName = findProvinceName(province);
        if (StringUtils.isNullOrEmpty(provinceName)) {
            provinceName = "";
        }

        cityName = StringUtils.isNullOrEmpty(cityName) ? "" : cityName;

        String areaName = findAreaName(area);
        if (StringUtils.isNullOrEmpty(areaName)) {
            areaName = "";
        }

        address = StringUtils.isNullOrEmpty(address) ? "" : address;

        return provinceName + cityName + areaName + address;
    }

    /**
     * areaCode 查询对应的areaName
     *
     * @param area areaCode
     * @return
     */
    public String findAreaName(String area) throws Exception {

        JdbcTemplate template = DataSourceManager.getJdbcTemplate();

        Map<String, Object> areaMap = new HashMap<>();
        try {

            Object[] params;
            String sql = "select id,code,name,cityCode" +
                    "    from t_dictionary_area " +
                    "    where code = ? ";
            params = new Object[]{area};
            sql += " order by id asc";

            areaMap = template.findOne(sql, params, MapHandler.CASESENSITIVE);
            if (areaMap == null) {
                return "";
            }

        } catch (Exception e) {
            L.error("[t_dictionary_area]查询失败:" + e.getMessage());
            throw e;
        }
        return FormatUtil.parseString(areaMap.get("name"));
    }

    /**
     * 根据cityCode 查询对应的cityName
     *
     * @param city cityCode
     * @return
     */
    public String findCityName(String city) throws Exception {
        JdbcTemplate template = DataSourceManager.getJdbcTemplate();

        Map<String, Object> cityMap = new HashMap<>();
        try {

            Object[] params;
            String sql = "select id,code,name,provincecode" +
                    "    from t_dictionary_city " +
                    "    where code = ? ";
            params = new Object[]{city};
            sql += " order by id asc";

            cityMap = template.findOne(sql, params, MapHandler.CASESENSITIVE);
            if (cityMap == null) {
                return "";
            }

        } catch (Exception e) {
            L.error("[t_dictionary_city]查询失败:" + e.getMessage());
            throw e;
        }
        return FormatUtil.parseString(cityMap.get("name"));
    }


    /**
     * 根据provincCode 查询对应的 provinceName
     *
     * @param province provinceCode
     * @return
     */
    public String findProvinceName(String province) throws Exception {
        JdbcTemplate template = DataSourceManager.getJdbcTemplate();

        Map<String, Object> provMap;
        try {

            Object[] params;
            String sql = "select id,code,name" +
                    "    from t_dictionary_province " +
                    "    where code = ? ";
            params = new Object[]{province};
            sql += " order by id asc";

            provMap = template.findOne(sql, params, MapHandler.CASESENSITIVE);

            if (provMap == null) {
                return "";
            }

        } catch (Exception e) {
            L.error("[t_dictionary_province]查询失败:" + e.getMessage());
            throw e;
        }
        return FormatUtil.parseString(provMap.get("name"));
    }


    public static void main(String[] args) throws Exception {
        ProjectDao dao = new ProjectDao();
        String provName = dao.findProvinceName("1100001");
        System.out.println(provName);
        String cityName = dao.findCityName("110100");
        System.out.println(cityName);
        String areaName = dao.findAreaName("110101");
        System.out.println(areaName);
        String categoryName = dao.findCategoryNames("00");
        System.out.println(categoryName);
    }
}

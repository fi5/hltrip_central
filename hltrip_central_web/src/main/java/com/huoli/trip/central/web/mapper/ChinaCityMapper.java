package com.huoli.trip.central.web.mapper;

import com.huoli.trip.common.entity.ChinaCity;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChinaCityMapper {

    @Select("SELECT code,name FROM china_city where name LIKE #{condition} AND type = #{type} limit #{count}")
    List<ChinaCity> queryCityByNameCondition(String condition, int type, int count);

    @Select("SELECT code,name FROM china_city where pinying LIKE #{condition} AND type = #{type} limit #{count}")
    List<ChinaCity> queryCityByPinyinCondition(String condition, int type, int count);

    @Select("SELECT code,name FROM china_city where name = #{name} AND type = #{type} limit 1")
    ChinaCity getByName(String name, int type);
}

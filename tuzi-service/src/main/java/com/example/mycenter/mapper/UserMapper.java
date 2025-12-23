package com.example.mycenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mycenter.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper 接口（继承 MyBatis-Plus 的 BaseMapper，无需手动写SQL）
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // MyBatis-Plus 的 BaseMapper 已内置以下常用方法，无需手动实现：
    // - insert(T entity)：新增
    // - updateById(T entity)：根据ID更新
    // - selectById(Serializable id)：根据ID查询
    // - selectOne(Wrapper<T> queryWrapper)：条件查询单条
    // - selectList(Wrapper<T> queryWrapper)：条件查询列表
    // 如需自定义SQL，可在此接口添加方法，并编写对应的 Mapper.xml
}
package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;

import com.xuecheng.model.dto.*;
import com.xuecheng.model.dto.QueryCourseParamsDto;
import com.xuecheng.model.po.CourseBase;

import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.model.po.CourseBase;
import com.xuecheng.model.po.CourseCategory;
import com.xuecheng.model.po.CourseMarket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/10/8 9:46
 */
@Slf4j
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    CourseBaseMapper courseBaseMapper;


    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Autowired
    CourseMarketServiceImpl courseMarketService;



    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams params, QueryCourseParamsDto queryCourseParamsDto) {
        LambdaQueryWrapper<CourseBase> wrapper=new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),CourseBase::getName,queryCourseParamsDto.getCourseName());
        wrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()),CourseBase::getStatus,queryCourseParamsDto.getPublishStatus());
        wrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,queryCourseParamsDto.getAuditStatus());

        Page<CourseBase> page = new Page<>(params.getPageNo(), params.getPageSize());

        Page<CourseBase> pageResult= courseBaseMapper.selectPage(page,wrapper);
        List<CourseBase> records = pageResult.getRecords();
        long total = pageResult.getTotal();
        PageResult<CourseBase> pageResult1= new PageResult<>(records,total, params.getPageNo(), params.getPageSize());
        return pageResult1;
    }

    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
        //校验数据库合法性
//        if(StringUtils.isBlank(dto.getName())){
//            //抛出异常
////            throw  new RuntimeException("课程名称为空");
//            XueChengPlusException.cast("课程名称为空");
////            XueChengPlusException.cast(CommonError.PARAMS_ERROR);
//        }
//
//        if (StringUtils.isBlank(dto.getMt())) {
//            throw new RuntimeException("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getSt())) {
//            throw new RuntimeException("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getGrade())) {
//            throw new RuntimeException("课程等级为空");
//        }
//
//        if (StringUtils.isBlank(dto.getTeachmode())) {
//            throw new RuntimeException("教育模式为空");
//        }
//
//        if (StringUtils.isBlank(dto.getUsers())) {
//            throw new RuntimeException("适应人群为空");
//        }
//
//        if (StringUtils.isBlank(dto.getCharge())) {
//            throw new RuntimeException("收费规则为空");
//        }


        //向Course_base课程基本信息表表添加数据
        CourseBase courseBase = new CourseBase();

//        courseBase.setName(dto.getName());
//        courseBase.setGrade(dto.getGrade());
        //以上设置数据的方法可以拷贝,从源拷贝到目标
        BeanUtils.copyProperties(dto,courseBase);
        //设置机构id
        courseBase.setCompanyId(companyId);
        //创建时间
        courseBase.setCreateDate(LocalDateTime.now());
        //审核状态默认未提交
        courseBase.setAuditStatus("202002");
        //发布状态默认为未发布
        courseBase.setStatus("203001");
        //插入成功返回1
        int insert = courseBaseMapper.insert(courseBase);
        //得到课程id
        Long courseId = courseBase.getId();

        //向数据库插入课程基本信息表，拿到课程的id
        //向课程营销表添加数据
        CourseMarket courseMarket = new CourseMarket();
        //两个对象的属性名一致，类型一样
        BeanUtils.copyProperties(dto,courseMarket);
        courseMarket.setId(courseId);
        //校验如果课程为收费，必须输入价格且大于0
//        String charge = courseMarket.getCharge();
//        if(charge.equals("201001")){
//            if(courseMarket.getPrice()==null || courseMarket.getPrice().floatValue()<=0){
////                throw new RuntimeException("课程为收费价格不能为空且必须大于0");
//                XueChengPlusException.cast("课程为收费价格不能为空且必须大于0");
//
//            }
//        }
        //向数据库插入课程营销表
        int insert1 = this.saveCourseMarket(courseMarket);
        //插入成功返回1
//        int insert1 = courseMarketMapper.insert(courseMarket);
//        if(insert<1 && insert1<1){
        //只要有一个插入不成功抛出异常
        if(insert<1 || insert1<1){
            log.error("创建课程过程中出错:{}",dto);
            throw new RuntimeException("创建课程过程中出错");
        }

        //返回

        return getCourseBaseInfo(courseId);
    }

    /**
     * 根据课程id查询课程的基本和营销信息
     * @param courseId 课程id
     * @return 课程的信息
     */
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId){

        //基本信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);

        //营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);

        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);

        //根据课程分类的id查询分类的名称
        String mt = courseBase.getMt();
        String st = courseBase.getSt();

        CourseCategory mtCategory = courseCategoryMapper.selectById(mt);
        CourseCategory stCategory = courseCategoryMapper.selectById(st);
        if(mtCategory!=null){
            //分类名称
            String mtName = mtCategory.getName();
            courseBaseInfoDto.setMtName(mtName);
        }
        if(stCategory!=null){
            //分类名称
            String stName = stCategory.getName();
            courseBaseInfoDto.setStName(stName);
        }


        return courseBaseInfoDto;

    }

    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto dto) {
        //校验
        //课程id
        Long id = dto.getId();
        CourseBase courseBase = courseBaseMapper.selectById(id);
        if(courseBase==null){
            XueChengPlusException.cast("课程不存在");
        }

        //校验本机构只能修改本机构的课程
        if(!courseBase.getCompanyId().equals(companyId)){
            XueChengPlusException.cast("本机构只能修改本机构的课程");
        }

        //封装基本信息的数据
        BeanUtils.copyProperties(dto,courseBase);
        courseBase.setChangeDate(LocalDateTime.now());

        //更新课程基本信息
        int i = courseBaseMapper.updateById(courseBase);

        //封装营销信息的数据
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(dto,courseMarket);

        //校验如果课程为收费，必须输入价格且大于0
//        String charge = courseMarket.getCharge();
//        if(charge.equals("201001")){
//            if(courseMarket.getPrice()==null || courseMarket.getPrice().floatValue()<=0){
////                throw new RuntimeException("课程为收费价格不能为空且必须大于0");
//                XueChengPlusException.cast("课程为收费价格不能为空且必须大于0");
//
//            }
//        }

        //请求数据库
        //对营销表有则更新，没有则添加
//        boolean b = courseMarketService.saveOrUpdate(courseMarket);

        saveCourseMarket(courseMarket);
        //查询课程信息
        CourseBaseInfoDto courseBaseInfo = this.getCourseBaseInfo(id);
        return courseBaseInfo;
    }

    //抽取对营销的保存
    private int saveCourseMarket(CourseMarket courseMarket){


        String charge = courseMarket.getCharge();
        if(StringUtils.isBlank(charge)){
            XueChengPlusException.cast("收费规则没有选择");
        }
        if(charge.equals("201001")){
            if(courseMarket.getPrice()==null || courseMarket.getPrice().floatValue()<=0){
//                throw new RuntimeException("课程为收费价格不能为空且必须大于0");
                XueChengPlusException.cast("课程为收费价格不能为空且必须大于0");

            }
        }

        //保存
        boolean b = courseMarketService.saveOrUpdate(courseMarket);

        return b?1:0;

    }



}
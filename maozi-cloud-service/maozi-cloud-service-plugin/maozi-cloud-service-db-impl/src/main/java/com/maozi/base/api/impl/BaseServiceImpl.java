package com.maozi.base.api.impl;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.shaded.com.google.common.collect.Sets;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.toolkit.MPJWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.google.common.collect.Lists;
import com.maozi.base.AbstractBaseCode;
import com.maozi.base.AbstractBaseDomain;
import com.maozi.base.AbstractBaseDtomain;
import com.maozi.base.AbstractBaseNameDomain;
import com.maozi.base.api.IBaseMapper;
import com.maozi.base.api.rpc.BaseServiceResult;
import com.maozi.base.enums.Status;
import com.maozi.base.enums.StoreClassType;
import com.maozi.base.param.PageParam;
import com.maozi.base.param.SaveUpdateBatch;
import com.maozi.base.param.plugin.OrderParam;
import com.maozi.base.param.plugin.TimeParam;
import com.maozi.base.plugin.StoreClass;
import com.maozi.base.plugin.join.JoinBaseType;
import com.maozi.base.plugin.join.JoinPlugins;
import com.maozi.base.plugin.mapping.QueryMapping;
import com.maozi.base.plugin.query.QueryBaseType;
import com.maozi.base.plugin.query.QueryPlugin;
import com.maozi.base.plugin.type.JoinType;
import com.maozi.base.plugin.type.QueryType;
import com.maozi.base.result.DropDownResult;
import com.maozi.base.result.PageResult;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.utils.SpringUtil;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class BaseServiceImpl<M extends IBaseMapper<T>, T extends AbstractBaseDomain,D extends AbstractBaseDtomain, E extends AbstractBaseCode> extends MPJBaseServiceImpl<M, T ,E> implements MPJBaseService<T>,BaseServiceResult<D> {

	protected Class<T> doClass;
	
	protected Class<D> dtoClass;
	
	protected abstract String getAbbreviationModelName();
	
	public BaseServiceImpl() {
		
		for(Class<?> superClass = this.getClass();;superClass = superClass.getSuperclass()) {
		
		Type genericSuperclass = superClass.getGenericSuperclass();
		
			if(genericSuperclass instanceof ParameterizedType) {
			
				ParameterizedType type = (ParameterizedType) genericSuperclass;

				doClass = (Class<T>) type.getActualTypeArguments()[1];
			
				dtoClass = (Class<D>) type.getActualTypeArguments()[2];
				
				break ;
			
			}
		
		}
		
	}

    @Override
    public Class<T> currentModelClass() {
        return super.currentModelClass();
    }
    
    public Class<D> getDtoClass(){
    
    	if(dtoClass.getName().equals(Void.class.getName())) {
    		throw new BusinessResultException(getBaseCodes().NOT_SET_RESPONSE_ERROR,500);
    	}
    	
    	return dtoClass;
    
    }
    
    protected <D> com.baomidou.mybatisplus.extension.plugins.pagination.Page<D> convertPage(PageParam pageParam){
    	return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<D>(pageParam.getCurrent(),pageParam.getSize());
    }
    
    protected <D> PageResult convertPageResult(Page<T> page,Supplier<D> target) {
		return new PageResult<D>(page.getCurrent(),page.getSize(),page.getTotal(),copyList(page.getRecords(),target));
	}
    
    protected <D> PageResult convertPageResult(Page<T> page,List<D> responseDatas) {
		return new PageResult<D>(page.getCurrent(),page.getSize(),page.getTotal(),responseDatas);
	}

	protected <D> PageResult convertPageResult(Page<D> page) {
		return new PageResult<D>(page.getCurrent(),page.getSize(),page.getTotal(),page.getRecords());
	}
    
    protected <D> Map<Long,D> toMapByIds(List<D> datas,Function<D, Long> function) {
    	return datas.stream().collect(Collectors.toMap(function, Function.identity()));
	}
    
    protected <D> String getColumn(SFunction<D, ?> sfunction) {

    	try {

    		Method method = sfunction.getClass().getDeclaredMethod("writeReplace");

            method.setAccessible(true);

            SerializedLambda serializedLambda = (SerializedLambda) method.invoke(sfunction);

            String fieldWithGet = serializedLambda.getImplMethodName();

            return StrUtil.toUnderlineCase(fieldWithGet.substring(3));

    	}catch (Exception e) {

			throwSystemError(e);

			return null;

		}

	}
    
    protected <D> String[] getColumns(SFunction<D, ?> ... sfunctions) {
    	
    	int i = 0;
    	
    	String [] columns = new String[sfunctions.length];
    	
    	for(SFunction<D, ?> sfunction : sfunctions) {
	        
	        columns[i] = getColumn(sfunction);
	        
	        ++i;
	        
		}
    	
    	return columns;
    	
	}

	protected <D> String[] getColumns(Class clazz) {

		List<String> columns = Lists.newArrayList();

		for(;!Object.class.getName().equals(clazz.getName());clazz = clazz.getSuperclass()){

			Field [] fields = clazz.getDeclaredFields();

			for(Field field : fields) {

				QueryMapping annotation = field.getAnnotation(QueryMapping.class);

				if(isNull(annotation)) {
					columns.add("t."+StrUtil.toUnderlineCase(field.getName()));
				}else if(annotation.ignore() && ( isNotEmpty(annotation.field()) || isNotEmpty(annotation.tableName()))){

					String tableName = isNotEmpty(annotation.field()) ? annotation.field() : "t";

					String fieldName = isNotEmpty(annotation.tableName()) ? annotation.tableName() : StrUtil.toUnderlineCase(field.getName());

					columns.add(fieldName+"."+tableName);

				}

			}

		}

		return columns.toArray(new String[columns.size()]);

	}
    
    protected T getById(Long id,String ... columns){
    	
    	isNullThrowError(id, getAbbreviationModelName());
    	
    	MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
    	
    	if(columns.length > 0) {
    		wrapper.select(columns);
    	}
    	
    	wrapper.eq(getColumn(AbstractBaseDomain::getId),id);
    	
    	return getOne(wrapper);
    	
	}
    
    protected T getById(Long id,SFunction<T, ?> ... columns){
    	
    	isNullThrowError(id, getAbbreviationModelName());
    	
    	MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
    	
    	if(columns.length > 0) {
    		wrapper.select(columns);
    	}
    	
    	wrapper.eq(getColumn(AbstractBaseDomain::getId),id);
    	
    	return getOne(wrapper);
    	
	}
    
    protected void checkAvailable(Long id){
    	
    	isNullThrowError(id, getAbbreviationModelName());
    	
    	MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
    	
    	wrapper.select(getColumn(AbstractBaseDomain::getStatus));
    	
    	wrapper.eq(getColumn(AbstractBaseDomain::getId),id);
    	
    	T domain = getOne(wrapper);
    	
    	isNullThrowError(domain, getAbbreviationModelName());
    	
    	checkBoolThrowError(domain.getStatus() == Status.enable,getAbbreviationModelName(),getBaseCodes().FORBIDDEN_ERROR);
    	
	}
    
    protected void checkAvailable(List<Long> ids){
    	
    	collectionIsEmptyThrowError(ids,getAbbreviationModelName()+"列表");
    	
    	MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin(); 
    	
    	wrapper.select(getColumn(AbstractBaseDomain::getStatus));
    	
    	wrapper.in(getColumn(AbstractBaseDomain::getId),ids);
    	
    	List<T> domains = list(wrapper);
    	
    	domains.parallelStream().forEach((domain)->{
    		
    		if(domain.getStatus() == Status.disable) {
        		throw new BusinessResultException(getAbbreviationModelName(),getBaseCodes().FORBIDDEN_ERROR,200);
        	}
    		
    	});
    	
	}
    
    protected T getAvailableById(Long id,String ... columns){
    	
    	isNullThrowError(id, getAbbreviationModelName());
    	
    	MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
    	
    	wrapper.select(getColumn(AbstractBaseDomain::getStatus));
    	
    	wrapper.select(columns);
    	
    	wrapper.eq(getColumn(AbstractBaseDomain::getId),id);
    	
    	T domain = getOne(wrapper);
    	
    	isNullThrowError(domain, getAbbreviationModelName());
    	
    	if(domain.getStatus() == Status.disable) {
    		throw new BusinessResultException(getAbbreviationModelName(),getBaseCodes().FORBIDDEN_ERROR,200);
    	}
    	
    	return domain;
    	
	}
    
    protected T getAvailableByParam(MPJLambdaWrapper<T> wrapper){
    	
    	wrapper.select(getColumn(AbstractBaseDomain::getStatus));
    	
    	T domain = getOne(wrapper);
    	
    	isNullThrowError(domain, getAbbreviationModelName());
    	
    	if(domain.getStatus() == Status.disable) {
    		throw new BusinessResultException(getAbbreviationModelName(),getBaseCodes().FORBIDDEN_ERROR,200);
    	}
    	
    	return domain;
    	
	}
    
    protected void checkHas(Wrapper<T> wrapper) {
    	checkBoolThrowError(count(wrapper) > 0, getAbbreviationModelName(),getBaseCodes().DATA_NOT_EXIST_ERROR);
	}
    
    protected void checkNotHas(Wrapper<T> wrapper) {
    	checkBoolThrowError(count(wrapper) < 1, getAbbreviationModelName(),getBaseCodes().DATA_EXIST_ERROR);
	}
    
    protected DropDownResult dropDown(Long id){
    	
    	isNullThrowError(id, getAbbreviationModelName());
    	
    	MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
    	
    	wrapper.select(getColumns(AbstractBaseNameDomain::getId,AbstractBaseNameDomain::getName));
    	
    	wrapper.eq(getColumn(AbstractBaseDomain::getId), id);
    	
    	T domain = getOne(wrapper);
    	
    	isNullThrowError(domain, getAbbreviationModelName());
    	
    	return copy(domain, DropDownResult.class);
    	
	}
    
    protected List<DropDownResult> dropDownList(){
    	
    	MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
    	
    	wrapper.select(getColumns(AbstractBaseNameDomain::getId,AbstractBaseNameDomain::getName));
    	
    	wrapper.eq(getColumn(AbstractBaseDomain::getStatus), Status.enable);
    	
    	return copyList(list(wrapper), DropDownResult::new);
    	
	}
    
    protected T getByIdThrowError(Long id,String ... columns){
    	
    	isNullThrowError(id, getAbbreviationModelName());
    	
    	MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
    	
    	if(columns.length > 0) {
    		wrapper.select(columns);
    	}
    	
    	wrapper.eq(getColumn(AbstractBaseDomain::getId),id);
    	
    	T domain = getOne(wrapper);
    	
    	isNullThrowError(domain,getAbbreviationModelName());
    	
    	return domain;
    	
	}

	protected T getByIdThrowError(Long id,MPJLambdaWrapper<T> wrapper){

		isNullThrowError(id, getAbbreviationModelName());

		wrapper.eq(getColumn(AbstractBaseDomain::getId),id);

		T domain = getOne(wrapper);

		isNullThrowError(domain,getAbbreviationModelName());

		return domain;

	}
    
    protected <D> D getByParamThrowError(Wrapper<T> wrapper,Class<D> clazz) {
		
    	T domain = getOne(wrapper);
		
		isNullThrowError(domain, getAbbreviationModelName());
		
		return copy(domain, clazz);
    	
	}
    
    protected <D> D getByParamThrowErrorRelation(Wrapper<T> wrapper,Class<D> clazz) {
		
    	T domain = getOne(wrapper);
		
		isNullThrowError(domain, getAbbreviationModelName());
		
		D response = copy(domain, clazz);
		
		setRelationData(response, clazz);
		
		return response;
    	
	}
    
    protected <D> T getByIdThrowError(Long id,SFunction<D, ?> ... columns) {
    	return getByIdThrowError(id,getColumns(columns));
	}
    
    protected <V,D> V getByIdThrowError(Long id,Class<V> clazz,SFunction<D, ?> ... columns){
    	return copy(getByIdThrowError(id,columns), clazz);
	}
    
    protected <V> V getByIdThrowErrorRelation(Long id,Class<V> clazz) {

		V response = copy(getByIdThrowError(id,getColumns(clazz)), clazz);
    	
    	setRelationData(response, clazz);
    	
    	return response;
    	
	}

	protected <V,D> V getByParamThrowErrorRelation(D dto,Class<V> clazz) {

		isNullThrowError(dto, getAbbreviationModelName());

		MPJLambdaWrapper<T> wrapper = buildQueryWrapper(dto,clazz);

		wrapper.select(getColumns(clazz));

		V response = copy(getOne(wrapper), clazz);

		isNullThrowError(response, getAbbreviationModelName());

		setRelationData(response, clazz);

		return response;

	}
    
    protected List<T> listByIds(Collection<?> ids,String ... columns) {
    	
    	collectionIsEmptyThrowError(ids,getAbbreviationModelName()+"列表");
    	
    	MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
    	
    	if(columns.length > 0) {
    		wrapper.select(columns);
    	}
    	
    	wrapper.in(getColumn(AbstractBaseDomain::getId),ids);
    	
    	return list(wrapper);
    	
	}
    
    protected <D,V> List<V> list(Supplier<V> target,SFunction<D, ?> ... columns) {
    	
    	MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
    	
    	if(columns.length > 0) {
    		wrapper.select(columns);
    	}
		
    	List<T> list = list(wrapper);
    	
    	return copyList(list, target);
    	
	}
    
    protected <D> List<D> list(Wrapper<T> wrapper,Supplier<D> target) {
		
    	List<T> list = list(wrapper);
    	
    	return copyList(list, target);
    	
	}

	protected <V> List<V> list(D dto,Supplier<V> target) {

		isNullThrowError(dto, getAbbreviationModelName());

		V vo = target.get();

		List<T> list = list(buildQueryWrapper(dto, vo.getClass()));

		return copyList(list, () -> vo);

	}

	protected <V> List<V> listRelation(D dto,Supplier<V> target) {

		isNullThrowError(dto, getAbbreviationModelName());

		V vo = target.get();

		List<T> list = list(buildQueryWrapper(dto,vo.getClass()));

		setRelationData(dto,vo.getClass());

		return copyList(list, () -> vo);

	}
    
    protected <V> PageResult<V> list(PageParam pageParam,Wrapper<T> wrapper,Supplier<V> target) {
		
    	Page<T> page = page(convertPage(pageParam),wrapper);
    	
    	return convertPageResult(page, target);
    	
	}
    
    protected <V> PageResult<V> list(PageParam pageParam,Class<V> clazz) {
		
    	Page<V> page = selectJoinListPage(convertPage(pageParam),clazz,buildQueryWrapper(pageParam.getData(),clazz));
    	
    	return convertPageResult(page);
    	
	}
    
    protected Page<T> list(PageParam pageParam,Wrapper<T> wrapper) {
    	return page(convertPage(pageParam),wrapper);
	}
    
    protected <V> PageResult<V> listRelation(PageParam pageParam,Class<V> clazz) {
    	
    	PageResult<V> page = list(pageParam,clazz);
		
		setRelationData(page, clazz);
		
		return page;
		
	}

	protected <D> Long getCountByParam(D dto){

		isNullThrowError(dto,getAbbreviationModelName());

		return count(buildQueryWrapper(dto));

	}
    
    public boolean saveUpdate(T domain) {
    	
    	isNullThrowError(domain,getAbbreviationModelName());
    	
    	if(isNotNull(domain.getId())) {
    		
    		MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
    		
    		wrapper.eq(getColumn(AbstractBaseDomain::getId), domain.getId());
    		
    		checkBoolThrowError(count(wrapper) > 0,getAbbreviationModelName(),getBaseCodes().DATA_NOT_EXIST_ERROR);
    		
    	}
    	
		return super.saveOrUpdate(domain);
		
	}
    
    
	public Long saveUpdate(Long id,AbstractBaseDtomain param) {
	    	
    	isNullThrowError(param,getAbbreviationModelName());
    	
    	T domain = copy(param, doClass);
    	
    	if(isNotNull(id)) {
    		
    		domain.setId(id);
    		
    		MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
    		
    		wrapper.eq(getColumn(AbstractBaseDomain::getId), domain.getId());
    		
    		checkBoolThrowError(count(wrapper) > 0,getAbbreviationModelName(),getBaseCodes().DATA_NOT_EXIST_ERROR);
    		
    	}else {
    		validate(param);
    	}
    	
    	super.saveOrUpdate(domain);
    	
    	return domain.getId();
		
	}
	
    public void saveUpdateBatch(List<T> domains) {
    	
    	collectionIsEmptyThrowError(domains,getAbbreviationModelName()+"列表");

		Consumer<T> consumer = wrapConsumer((domain)->{

			if(isNotNull(domain.getId())) {

				MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();

				wrapper.eq(getColumn(AbstractBaseDomain::getId), domain.getId());

				checkBoolThrowError(count(wrapper) > 0,getAbbreviationModelName(),getBaseCodes().DATA_NOT_EXIST_ERROR);

			}

			super.saveOrUpdate(domain);

		});
    	
    	domains.parallelStream().forEach(consumer);
    	
    }
    
    protected void checkBind(Long id) {};
    
    protected void unbind(Long id) {};
    
    public void removeById(Long id) {
    	
    	isNullThrowError(id,getAbbreviationModelName());
		
		checkBind(id);
		
		unbind(id);
		
		super.removeById(id);
    	
    }
    
    @Override
    public AbstractBaseResult<D> getByIdResult(Long id,String ... colums) {
		return success(copy(getById(id, colums), getDtoClass()));
	}

    @Override
	public AbstractBaseResult<Map<Long,D>> listByIdsResult(Collection<Long> ids,String ... columns) {

		return success(copyList(listByIds(ids,columns), () -> {
			
			return ReflectUtil.newInstance(getDtoClass());

		}).stream().collect(Collectors.toMap((rpcResultData)->{
			
			return ReflectUtil.invoke(rpcResultData, "getId");
			
		},Function.identity())));

	}
    
    @Override
	public AbstractBaseResult<Map<Long,List<D>>> listByRelationIdsResult(Collection<Long> ids,String relationField,String ... columns) {
    	
    	collectionIsEmptyThrowError(ids,getAbbreviationModelName()+"列表");
    	
    	isNullThrowError(relationField, getAbbreviationModelName()+"分组");
    	
    	MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
    	
    	if(columns.length > 0) {
    		wrapper.select(columns);
    	}
    	
    	wrapper.in(relationField,ids);

		return success(copyList(list(wrapper), () -> {
			
			return ReflectUtil.newInstance(getDtoClass());

		}).stream().collect(Collectors.groupingBy((rpcResultData)->{
			
			return ReflectUtil.invoke(rpcResultData, "get"+StrUtil.upperFirst(relationField));
			
		},Collectors.toList())));

	}
    
    @Override
	public AbstractBaseResult<List<D>> listByRelationIdResult(Long id,String relationField,String ... columns) {
    	
    	isNullThrowError(id,getAbbreviationModelName());
    	
    	isNullThrowError(relationField, getAbbreviationModelName()+"分组");
    	
    	MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
    	
    	if(columns.length > 0) {
    		wrapper.select(columns);
    	}
    	
    	wrapper.eq(relationField,id);

		return success(copyList(list(wrapper), () -> {
			
			return ReflectUtil.newInstance(getDtoClass());

		}));

	}
	
    @Override
	public AbstractBaseResult<Long> getCountByParamResult(D dto){
		return success(getCountByParam(dto));
	}
	
	@Override
	public <D extends AbstractBaseDtomain> AbstractBaseResult<Long> saveUpdateResult(Long id,D param){
		return success(saveUpdate(id,param));
	}
	
	@Override
	public AbstractBaseResult<Void> saveUpdateBatchResult(List<SaveUpdateBatch> dtos){
		
		List<T> dos = copyList(collectionIsEmptyThrowError(dtos, getAbbreviationModelName()+"列表"), ()->{
			
			return ReflectUtil.newInstance(doClass);
			
		});
		
		saveUpdateBatch(dos);
		
		return success(null);
		
	}
	
	@Override
	public AbstractBaseResult<Void> removeByIdResult(Long id) {
		
		removeById(id);
		
		return success(null);
		
	}

	@Override
	public AbstractBaseResult<Void> removeByIdBatchResult(List<Long> ids) {
		
		collectionIsEmptyThrowError(ids, getAbbreviationModelName()+"列表");
		
		ids.parallelStream().forEach((id)->{
			removeById(id);
		});
		
		return success(null);
		
	}
	
	@Override
	public AbstractBaseResult<Void> checkAvailableResult(Long id){
		
		checkAvailable(id);
		
		return success(null);
		
	}
	
	@Override
	public AbstractBaseResult<Void> checkAvailableResult(List<Long> ids){
		
		checkAvailable(ids);
		
		return success(null);
		
	}
	
	@Override
	public AbstractBaseResult<D> getAvailableByIdResult(Long id,String ... columns){
    	
    	T domain = getAvailableById(id, columns);
    	
    	return success(copy(domain, dtoClass));
    	
	}
	
	@Override
	public AbstractBaseResult<DropDownResult> dropDownResult(Long id){
		return success(dropDown(id));
	}
	
	public AbstractBaseResult<List<DropDownResult>> dropDownListResult(){
		return success(dropDownList());
	}
	
	@Override
	public AbstractBaseResult<List<DropDownResult>> dropDownListResult(Collection<Long> ids){
		
		MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
    	
    	wrapper.select(getColumns(AbstractBaseNameDomain::getId,AbstractBaseNameDomain::getName));
    	
    	wrapper.in(getColumn(AbstractBaseDomain::getId), ids);
    	
    	return success(copyList(list(wrapper), DropDownResult::new));
		
	}
	
	
	public AbstractBaseResult<Void> updateStatus(Long id){
		
		checkBind(id);
		
		T domain = getByIdThrowError(id,getColumns(AbstractBaseDomain::getStatus));
		
		domain.setId(id);
		domain.setStatus(domain.getStatus() == Status.enable ? Status.disable : Status.enable);
		
		updateById(domain);
		
		return success(null);
		
	}
	
	private void paramFieldSetWrapper(String tableName,Object param,MPJLambdaWrapper<T> wrapper) {

		Class<?> paramClass = param.getClass();

		if (paramClass.isAnnotationPresent(JoinPlugins.class)) {

			JoinPlugins annotations = paramClass.getAnnotation(JoinPlugins.class);

			if(isNotNull(annotations.value())){

				Arrays.asList(annotations.value()).stream().forEach(annotation -> {

					JoinBaseType value = annotation.value();

					isNullThrowError(value, getAbbreviationModelName()+"连接类型");

					isNullThrowError(value.getType(), getAbbreviationModelName()+"连接类型");

					JoinType joinType = JoinType.get(value.getType());

					isNullThrowError(joinType, getAbbreviationModelName()+"连接类型");

					joinType.getJoinPlugin().apply(getAbbreviationModelName(),wrapper,annotation);

				});

			}

		}

		Field [] fields = ReflectUtil.getFields(param.getClass());
		
		for(Field field : fields) {
			
			Object data = ReflectUtil.invoke(param, "get"+StrUtil.upperFirst(field.getName()));
			
			if(isNull(data)) {continue;}

			QueryPlugin annotation = field.getAnnotation(QueryPlugin.class);

			String fieldName = isNotNull(annotation) && isNotEmpty(annotation.field()) ? annotation.field() : field.getName();
			
			if(isNotNull(annotation)) {
				
				if(annotation.nest()) {
					
					paramFieldSetWrapper(annotation.tableName(),field,wrapper);
					
					continue;
					
				}
				
				QueryBaseType value = annotation.value();
				
				isNullThrowError(value, getAbbreviationModelName()+"查询类型");
				
				isNullThrowError(value.getType(), getAbbreviationModelName()+"查询类型");
				
				QueryType queryType = QueryType.get(value.getType());
				
				isNullThrowError(queryType, getAbbreviationModelName()+"查询类型");

                if(isNotEmpty(annotation.tableName())){
                    tableName = annotation.tableName();
                }

				if(isNotEmpty(tableName)){
					fieldName = tableName+"."+fieldName;
				}
				
				queryType.getQueryPlugin().apply(wrapper, fieldName , data);
				
			}
			
			if(data instanceof TimeParam timeParam) {
				
				if(isNotNull(timeParam.getStartTime())) {
					QueryType.ge.getQueryPlugin().apply(wrapper,fieldName,timeParam.getStartTime());
				}
				
				if(isNotNull(timeParam.getEndTime())) {
					QueryType.le.getQueryPlugin().apply(wrapper,fieldName,timeParam.getEndTime());
				}
				
			}
			
			
		}
		
	}

	protected void setQueryMappingWrapper(Class clazz,MPJLambdaWrapper<T> wrapper){

		for(;!Object.class.getName().equals(clazz.getName());clazz = clazz.getSuperclass()){

			Field [] fields = clazz.getDeclaredFields();

			for(Field field : fields) {

				QueryMapping annotation = field.getAnnotation(QueryMapping.class);

				if(isNotNull(annotation) && !annotation.ignore() && !annotation.isService()) {

					Class<?> type = field.getType();

					String tableName = annotation.tableName();

					throwSystemError(isNotEmpty(tableName),getAbbreviationModelName()+"映射关系名称");

					String fieldName = isNotEmpty(annotation.field()) ? annotation.field() : field.getName();

					Class<?> relationClazz = StoreClass.storeClassMap.get(StoreClassType.db).get(tableName);

					throwSystemError(isNotNull(relationClazz),getAbbreviationModelName()+"映射关系");

					throwSystemError(!type.isPrimitive(),getAbbreviationModelName()+"映射关系类型错误");

					if(type.equals(List.class)){

						wrapper.selectCollection(relationClazz,(response) -> {
							return ReflectUtil.invoke(response, "get"+StrUtil.upperFirst(field.getName()));
						});

					}else{

						wrapper.selectAssociation(relationClazz,(response) -> {
							return ReflectUtil.invoke(response, "get"+StrUtil.upperFirst(field.getName()));
						});

					}

				}

			}

		}

	}
	
	public MPJLambdaWrapper<T> buildQueryWrapper(Object param,Class fieldsClass) {
		
		if(param instanceof PageParam pageParam) {
			param = pageParam.getData();
		}
		
		isNullThrowError(param, getAbbreviationModelName()+"参数");
		
		MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
		
		if(isNotNull(fieldsClass)) {

			wrapper.select(getColumns(fieldsClass));

//			setQueryMappingWrapper(fieldsClass,wrapper);

		}
		
		paramFieldSetWrapper(null,param,wrapper);
		
		if(param instanceof OrderParam orderParam) {
			
			orderParam.initOrderParam();
			
			List<String> asc = orderParam.getOrderAscFieldsMap().get("t");
			
			List<String> desc = orderParam.getOrderDescFieldsMap().get("t");
			
			wrapper.orderByAscStr(asc.size() > 0,asc);
			
			wrapper.orderByDescStr(desc.size() > 0,desc);
			
		}
		
		return wrapper;
		
	}
	
	public MPJLambdaWrapper<T> buildQueryWrapper(Object param) {
		return buildQueryWrapper(param,null);
	}
	
	public void setRelationData(Object data,Class clazz){
		
		if(data instanceof PageResult<?> pageResult) {
			data = pageResult.getData();
		}
		
		if(data instanceof List responses) {
			
			if(collectionIsNotEmpty(responses)) {
				
				Field [] fields = ReflectUtil.getFields(clazz);
				
				for(Field field : fields) {
					
					QueryMapping annotation = field.getAnnotation(QueryMapping.class);
					
					if(isNull(annotation) || annotation.ignore()) {
						continue;
					}
					
					String functionName = annotation.functionName();
					
					String relationFieldName = annotation.relationField();
					
					if(isEmpty(relationFieldName) && isEmpty(functionName)) {
						throwSystemError(getAbbreviationModelName()+"关系字段未设置");
					}
					
					if(annotation.isService()) {
						
						Object service = SpringUtil.getBean(annotation.serviceName());
						
						if(isNull(service)) {
							throwSystemError(getAbbreviationModelName()+"服务不存在");
						}
						
						Class<?> type = field.getType();
						
						if(isNotEmpty(functionName)) {
							
							Set param = Sets.newHashSet();
							
							for(Object response : responses) {
								param.add(ReflectUtil.invoke(response, "get"+StrUtil.upperFirst(relationFieldName)));
							}
							
							if(collectionIsNotEmpty(param)) {
								
								Map<Long,Object> result = ReflectUtil.invoke(service,functionName,param,relationFieldName);
								
								for(Object response : responses) {
									
									Long id = ReflectUtil.invoke(response, "get"+StrUtil.upperFirst(relationFieldName));
									
									Object object = result.get(id);
									
									if(isNotNull(object)) {
										ReflectUtil.invoke(response, "set"+StrUtil.upperFirst(field.getName()),object);
									}
									
								}
								
							}
						
						}else if(type.equals(List.class)) {
							
							Set<Long> relationIds = Sets.newHashSet();
							
							for(Object response : responses) {
								relationIds.add(ReflectUtil.invoke(response, "getId"));
							}
							
							if(collectionIsNotEmpty(relationIds)) {
								
								AbstractBaseResult<Map<Long,List>> rpcResult = ReflectUtil.invoke(service,"listByRelationIdsResult",relationIds,relationFieldName,getColumns(field.getClass()));
								
								Map<Long,List> rpcResultDatas = rpcResult.getResultDataThrowError();
								
								for(Object response : responses) {
									
									Long id = ReflectUtil.invoke(response, "getId");
									
									List list = rpcResultDatas.get(id);
									
									if(collectionIsNotEmpty(list)) {
										ReflectUtil.invoke(response, "set"+StrUtil.upperFirst(field.getName()),list);
									}
									
								}
								
							}
							
						}else if(type.equals(DropDownResult.class)) {
							
							Set<Long> relationIds = Sets.newHashSet();
							
							for(Object response : responses) {
								relationIds.add(ReflectUtil.invoke(response, "get"+StrUtil.upperFirst(relationFieldName)));
							}
							
							if(collectionIsNotEmpty(relationIds)) {
								
								AbstractBaseResult<List<DropDownResult>> rpcResult = ReflectUtil.invoke(service,"dropDownListResult",relationIds);
								
								List<DropDownResult> rpcResultDatas = rpcResult.getResultDataThrowError();
								
								Map<Long, DropDownResult> relationsMap = toMapByIds(rpcResultDatas,DropDownResult::getId);
								
								for(Object response : responses) {
									
									Long id = ReflectUtil.invoke(response, "get"+StrUtil.upperFirst(relationFieldName));
									
									DropDownResult relation = relationsMap.get(id);
									
									if(isNotNull(relation)) {
										ReflectUtil.invoke(response, "set"+StrUtil.upperFirst(field.getName()),relation);
									}
									
								}
								
							}
							
						}else {
							
							Set<Long> relationIds = Sets.newHashSet();
							
							for(Object response : responses) {
								relationIds.add(ReflectUtil.invoke(response, "get"+StrUtil.upperFirst(relationFieldName)));
							}
							
							if(collectionIsNotEmpty(relationIds)) {
								
								AbstractBaseResult<Map<Long,Object>> rpcResult = ReflectUtil.invoke(service,"listByIdsResult",relationIds,getColumns(field.getClass()));
								
								Map<Long,Object> rpcResultDatas = rpcResult.getResultDataThrowError();
								
								for(Object response : responses) {
									
									Long id = ReflectUtil.invoke(response, "get"+StrUtil.upperFirst(relationFieldName));
									
									Object relation =  rpcResultDatas.get(id);
									
									if(isNotNull(relation)) {
										ReflectUtil.invoke(response, "set"+StrUtil.upperFirst(field.getName()),relation);
									}
									
								}
								
							}
							
						}
						
					}
					
				}
				
			}
			
		}else {
			
			Field [] fields = ReflectUtil.getFields(clazz);
			
			for(Field field : fields) {
				
				QueryMapping annotation = field.getAnnotation(QueryMapping.class);
				
				if(isNull(annotation) || annotation.ignore()) {
					continue;
				}
				
				String relationFieldName = annotation.relationField();
				
				String functionName = annotation.functionName();
				
				if(isEmpty(relationFieldName) && isEmpty(functionName)) {
					throwSystemError(getAbbreviationModelName()+"关系字段未设置");
				}
				
				if(annotation.isService()) {
					
					Object service = SpringUtil.getBean(annotation.serviceName());
					
					if(isNull(service)) {
						throwSystemError(getAbbreviationModelName()+"服务不存在");
					}
					
					Class<?> type = field.getType();
					
					if(isNotEmpty(functionName)) {
						
						Object result = null;
						
						if(isNotEmpty(relationFieldName)) {
							
							var param = ReflectUtil.invoke(data, "get"+StrUtil.upperFirst(relationFieldName));
							
							result = ReflectUtil.invoke(service,functionName,param);
							
						}else{
							result = ReflectUtil.invoke(service,functionName);
						}
						
						if(isNotNull(result)) {
							ReflectUtil.invoke(data, "set"+StrUtil.upperFirst(field.getName()),result);
						}
					
					}else if(type.equals(List.class)) {
						
						Long id = ReflectUtil.invoke(data, "getId");
							
						AbstractBaseResult<List> rpcResult = ReflectUtil.invoke(service,"listByRelationIdResult",id,relationFieldName,getColumns(field.getClass()));
							
						List rpcResultDatas = rpcResult.getResultDataThrowError();
								
						if(collectionIsNotEmpty(rpcResultDatas)) {
							ReflectUtil.invoke(data, "set"+StrUtil.upperFirst(field.getName()),rpcResultDatas);
						}
						
					}else if(type.equals(DropDownResult.class)) {
						
						Long relationId = ReflectUtil.invoke(data, "get"+StrUtil.upperFirst(relationFieldName));
						
						if(isNotNull(relationId)) {
							
							AbstractBaseResult<DropDownResult> rpcResult = ReflectUtil.invoke(service,"dropDownResult",relationId);
							
							DropDownResult rpcResultData = rpcResult.getResultDataThrowError();
								
							if(isNotNull(rpcResultData)) {
								ReflectUtil.invoke(data, "set"+StrUtil.upperFirst(field.getName()),rpcResultData);
							}
							
						}
						
					}else {
						
						Long relationId = ReflectUtil.invoke(data, "get"+StrUtil.upperFirst(relationFieldName));
						
						if(isNotNull(relationId)) {
							
							AbstractBaseResult<Object> rpcResult = ReflectUtil.invoke(service,"getByIdResult",relationId,getColumns(field.getClass()));
							
							Object rpcResultDatas = rpcResult.getResultDataThrowError();
							
							if(isNotNull(rpcResultDatas)) {
								ReflectUtil.invoke(data, "set"+StrUtil.upperFirst(field.getName()),rpcResultDatas);
							}
							
						}
						
					}
					
				}
				
			}
			
		}
		
	}
	
}
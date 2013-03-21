/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.AttributeSerializer;
import pl.edu.icm.unity.db.json.AttributeTypeSerializer;
import pl.edu.icm.unity.db.mapper.AttributesMapper;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.AttributeBean;
import pl.edu.icm.unity.db.model.AttributeTypeBean;
import pl.edu.icm.unity.db.model.DBLimits;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.model.GroupElementBean;
import pl.edu.icm.unity.db.resolvers.AttributesResolver;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;


/**
 * Attributes related DB operations.
 * @author K. Benedyczak
 */
@Component
public class DBAttributes
{
	private DBLimits limits;
	private AttributesResolver attrResolver;
	private AttributeTypeSerializer atSerializer;
	private AttributeSerializer aSerializer;
	private GroupResolver groupResolver;
	private AttributeStatementProcessor statementsHelper;
	
	
	@Autowired
	public DBAttributes(DB db, AttributesResolver attrResolver,
			AttributeTypeSerializer atSerializer, AttributeSerializer aSerializer,
			GroupResolver groupResolver, AttributeStatementProcessor statementsHelper)
	{
		this.limits = db.getDBLimits();
		this.attrResolver = attrResolver;
		this.atSerializer = atSerializer;
		this.aSerializer = aSerializer;
		this.groupResolver = groupResolver;
		this.statementsHelper = statementsHelper;
	}

	public void addAttributeType(AttributeType toAdd, SqlSession sqlMap)
	{
		limits.checkNameLimit(toAdd.getName());
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		if (mapper.getAttributeType(toAdd.getName()) != null)
			throw new IllegalAttributeTypeException("The attribute type with name " + toAdd.getName() + 
					" already exists");
		
		AttributeTypeBean atb = new AttributeTypeBean(toAdd.getName(), atSerializer.toJson(toAdd), 
				toAdd.getValueType().getValueSyntaxId());
		mapper.insertAttributeType(atb);
	}

	
	public AttributeType getAttributeType(String id, SqlSession sqlMap)
	{
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		AttributeTypeBean atBean = attrResolver.resolveAttributeType(id, mapper);
		return attrResolver.resolveAttributeTypeBean(atBean);
	}
	
	public void removeAttributeType(String id, boolean withInstances, SqlSession sqlMap)
	{
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		if (mapper.getAttributeType(id) == null)
			throw new IllegalAttributeTypeException("The attribute type with name " + id + 
					" does not exist");
		if (!withInstances)
		{
			AttributeBean ab = new AttributeBean();
			ab.setName(id);
			if (mapper.getAttributes(ab).size() > 0)
				throw new IllegalAttributeTypeException("The attribute type " + id + " has instances");
		}
		mapper.deleteAttributeType(id);
	}
	
	public List<AttributeType> getAttributeTypes(SqlSession sqlMap)
	{
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		List<AttributeTypeBean> raw = mapper.getAttributeTypes();
		List<AttributeType> ret = new ArrayList<AttributeType>(raw.size());
		for (int i=0; i<raw.size(); i++)
		{
			AttributeTypeBean r = raw.get(i);
			ret.add(attrResolver.resolveAttributeTypeBean(r));
		}
		return ret;
	}
	
	
	private AttributeBean prepareAttributeParam(long entityId, long typeId, String attributeTypeName, String group, 
			AttributesMapper mapper, GroupsMapper groupsMapper)
	{
		GroupBean gr = groupResolver.resolveGroup(group, groupsMapper);
		AttributeBean param = new AttributeBean();
		param.setEntityId(entityId);
		param.setGroupId(gr.getId());
		param.setTypeId(typeId);
		param.setName(attributeTypeName);
		return param;
	}
	
	public void addAttribute(long entityId, Attribute<?> attribute, boolean update, SqlSession sqlMap)
	{
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		GroupsMapper grMapper = sqlMap.getMapper(GroupsMapper.class);

		AttributeTypeBean atBean = attrResolver.resolveAttributeType(attribute.getName(), mapper);
		AttributeType at = attrResolver.resolveAttributeTypeBean(atBean);
		AttributeValueChecker.validate(attribute, at);
		
		AttributeBean param = prepareAttributeParam(entityId, atBean.getId(), attribute.getName(),
				attribute.getGroupPath(), mapper, grMapper);
		List<AttributeBean> existing = mapper.getAttributes(param);
		if (existing.size() == 0)
		{
			if (grMapper.isMember(new GroupElementBean(param.getGroupId(), entityId)) == null)
				throw new IllegalGroupValueException("The entity is not a member of the group specified in the attribute");
			param.setValues(aSerializer.toJson(attribute));
			mapper.insertAttribute(param);
		} else
		{
			if (!update)
				throw new IllegalAttributeValueException("The attribute already exists");
			param.setValues(aSerializer.toJson(attribute));
			mapper.updateAttribute(param);
		}
	}
	
	public void removeAttribute(long entityId, String groupPath, String attributeTypeName, SqlSession sqlMap)
	{
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		GroupsMapper grMapper = sqlMap.getMapper(GroupsMapper.class);
		
		AttributeTypeBean atBean = attrResolver.resolveAttributeType(attributeTypeName, mapper);
		AttributeBean param = prepareAttributeParam(entityId, atBean.getId(), attributeTypeName,
				groupPath, mapper, grMapper);
		List<AttributeBean> existing = mapper.getAttributes(param);
		if (existing.size() == 0)
			throw new IllegalAttributeValueException("The attribute does not exist");
		
		mapper.deleteAttribute(param);
	}
	
	private List<String> getGroupsOrGroup(long entityId, String groupPath, GroupsMapper grMapper)
	{
		if (groupPath == null)
		{
			List<GroupBean> raw = grMapper.getGroups4Entity(entityId);
			List<String> ret = new ArrayList<String>();
			for (GroupBean gb: raw)
				ret.add(groupResolver.resolveGroupPath(gb, grMapper));
			return ret;
		} else
			return Collections.singletonList(groupPath);
	}
	
	/**
	 * Returns all attributes. Attribute name can be given or not.
	 * If the group is null, then attributes in all group scopes are returned.
	 * @param entityId
	 * @param groupPath
	 * @param attributeTypeName
	 * @param sql
	 * @return
	 */
	public Collection<Attribute<?>> getAllAttributes(long entityId, String groupPath,
			String attributeTypeName, SqlSession sql)
	{
		Map<String, Map<String, Attribute<?>>> asMap = getAllAttributesAsMap(entityId, groupPath, attributeTypeName, sql);
		List<Attribute<?>> ret = new ArrayList<Attribute<?>>();
		for (Map<String, Attribute<?>> entry: asMap.values())
			ret.addAll(entry.values());
		return ret;
	}
	
	public Map<String, Attribute<?>> getAllAttributesAsMapOneGroup(long entityId, String groupPath,
			String attributeTypeName, SqlSession sql)
	{
		if (groupPath == null)
			throw new IllegalArgumentException("For this method group must be specified");
		Map<String, Map<String, Attribute<?>>> asMap = getAllAttributesAsMap(entityId, groupPath, 
				attributeTypeName, sql);
		return asMap.get(groupPath);
	}
	
	/**
	 * See {@link #getAllAttributes(long, String, String, SqlSession)}, the only difference is that the result
	 * is returned in a map indexed with groups (1st key) and attribute names (submap key).
	 * @param entityId
	 * @param groupPath
	 * @param attributeTypeName
	 * @param sql
	 * @return
	 */
	public Map<String, Map<String, Attribute<?>>> getAllAttributesAsMap(long entityId, String groupPath,
			String attributeTypeName, SqlSession sql)
	{
		AttributesMapper atMapper = sql.getMapper(AttributesMapper.class);
		GroupsMapper gMapper = sql.getMapper(GroupsMapper.class);
		
		List<String> groups = getGroupsOrGroup(entityId, groupPath, gMapper);

		
		Set<String> allGroups = getAllGroups(entityId, gMapper);
		Map<String, Map<String, Attribute<?>>> directAttributesByGroup = createAllAttrsMap(entityId, 
				atMapper, gMapper);
		Map<String, Map<String, Attribute<?>>> ret = new HashMap<String, Map<String, Attribute<?>>>();
		for (String group: groups)
		{
			Map<String, Attribute<?>> inGroup = statementsHelper.getEffectiveAttributes(entityId, 
					group, attributeTypeName, allGroups, directAttributesByGroup, atMapper, gMapper);
			ret.put(group, inGroup);
		}
		return ret;
	}
	
	/**
	 * It is assumed that the attribute is single-value and mapped to string.
	 * Returned are all entities which has value of the attribute out of the given set.
	 * <p> 
	 * IMPORTANT! This is not taking into account effective attributes, and so it is usable only for certain system
	 * attributes.
	 * @param groupPath
	 * @param attributeTypeName
	 * @param value
	 * @param sql
	 * @return
	 */
	public Set<Long> getEntitiesBySimpleAttribute(String groupPath, String attributeTypeName, 
			Set<String> values, SqlSession sql)
	{
		GroupsMapper grMapper = sql.getMapper(GroupsMapper.class);
		AttributesMapper atMapper = sql.getMapper(AttributesMapper.class);

		GroupBean grBean = groupResolver.resolveGroup(groupPath, grMapper);
		List<AttributeBean> allAts = getDefinedAttributes(null, grBean.getId(), attributeTypeName, atMapper);
		
		Set<Long> ret = new HashSet<Long>();
		for (AttributeBean ab: allAts)
		{
			Attribute<?> attr = attrResolver.resolveAttributeBean(ab, groupPath);
			if (values.contains((String)attr.getValues().get(0)))
				ret.add(ab.getEntityId());
		}
		return ret;
	}
	
	
	
	private Set<String> getAllGroups(long entityId, GroupsMapper gMapper)
	{
		List<GroupBean> groups = gMapper.getGroups4Entity(entityId);
		Set<String> ret = new HashSet<String>();
		for (GroupBean group: groups)
			ret.add(groupResolver.resolveGroupPath(group, gMapper));
		return ret;
	}
	
	private Map<String, Map<String, Attribute<?>>> createAllAttrsMap(long entityId, AttributesMapper atMapper,
			GroupsMapper gMapper)
	{
		Map<String, Map<String, Attribute<?>>> ret = new HashMap<String, Map<String, Attribute<?>>>();
		List<AttributeBean> allAts = getDefinedAttributes(entityId, null, null, atMapper);
		for (AttributeBean ab: allAts)
		{
			String groupPath = groupResolver.resolveGroupPath(ab.getGroupId(), gMapper);
			Attribute<?> attribute = attrResolver.resolveAttributeBean(ab, groupPath);
			
			Map<String, Attribute<?>> attrsInGroup = ret.get(groupPath);
			if (attrsInGroup == null)
			{
				attrsInGroup = new HashMap<String, Attribute<?>>();
				ret.put(groupPath, attrsInGroup);
			}
			attrsInGroup.put(attribute.getName(), attribute);
		}
		return ret;
	}
	
	private List<AttributeBean> getDefinedAttributes(Long entityId, Long groupId, String attributeName, 
			AttributesMapper mapper)
	{
		AttributeBean param = new AttributeBean();
		param.setGroupId(groupId);
		param.setEntityId(entityId);
		param.setName(attributeName);
		return mapper.getAttributes(param);
	}
}














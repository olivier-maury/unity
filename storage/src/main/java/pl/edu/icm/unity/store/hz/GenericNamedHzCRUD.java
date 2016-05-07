/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.hz.rdbmsflush.RDBMSMutationEvent;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.types.NamedObject;

import com.hazelcast.core.TransactionalMap;

/**
 * Generic CRUD implementation on hazelcast map.
 * @author K. Benedyczak
 */
public abstract class GenericNamedHzCRUD<T extends NamedObject> extends GenericBasicHzCRUD<T> implements NamedCRUDDAO<T>
{
	public GenericNamedHzCRUD(String storeId, String name, String rdbmsCounterpartDaoName,
			BasicCRUDDAO<T> rdbmsDAO)
	{
		super(storeId, name, rdbmsCounterpartDaoName, rdbmsDAO);
	}

	@Override
	public long create(T obj) throws IllegalArgumentException
	{
		StorageLimits.checkNameLimit(obj.getName());
		TransactionalMap<String, Long> nameMap = getNameMap();
		if (nameMap.containsKey(obj.getName()))
			throw new IllegalArgumentException(name + " [" + obj.getName() + "] already exists");
		long key = super.create(obj);
		nameMap.put(obj.getName(), key);
		return key;
	}
	
	@Override
	public void update(T obj)
	{
		Long key = getNameMap().get(obj.getName());
		if (key == null)
			throw new IllegalArgumentException(name + " [" + obj.getName() + "] does not exists");
		updateByKey(key, obj);
	}

	@Override
	public void updateByKey(long id, T obj)
	{
		TransactionalMap<Long, T> hMap = getMap();
		T old = hMap.get(id);
		if (old == null)
			throw new IllegalArgumentException(name + " [" + id + "] does not exists");
		preUpdateCheck(old, obj);
		firePreUpdate(id, old.getName(), obj, old);
		if (!old.getName().equals(obj.getName()))
		{
			StorageLimits.checkNameLimit(obj.getName());
			TransactionalMap<String, Long> nameMap = getNameMap();
			nameMap.remove(old.getName());
			nameMap.put(obj.getName(), id);
		}
		hMap.put(id, obj);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, "update", obj));
	}
	
	@Override
	public void delete(String id)
	{
		Long removed = getNameMap().remove(id);
		if (removed == null)
			throw new IllegalArgumentException(name + " [" + id + "] does not exist");
		super.deleteByKey(removed);
	}
	
	@Override
	public void deleteByKey(long key)
	{
		deleteByKey(key, true);
	}
	
	protected void deleteByKey(long key, boolean fireEvent)
	{
		T removed = super.deleteByKeyRet(key, fireEvent);
		getNameMap().remove(removed.getName());
	}
	
	@Override
	public T get(String id)
	{
		Long key = getNameMap().get(id);
		if (key == null)
			throw new IllegalArgumentException(name + " [" + id + "] does not exists");
		return getMap().get(key);
	}

	@Override
	public boolean exists(String id)
	{
		return getNameMap().get(id) != null;
	}
	
	@Override
	public Map<String, T> getAsMap()
	{
		TransactionalMap<Long, T> hMap = getMap();
		TransactionalMap<String, Long> nameMap = getNameMap();
		Map<String, T> ret = new HashMap<>();
		for (String key: nameMap.keySet())
			ret.put(key, hMap.get(nameMap.get(key)));
		return ret;
	}

	@Override
	public void firePreRemove(long modifiedId, String modifiedName, T removed)
	{
		super.firePreRemove(modifiedId, removed.getName(), removed);
	}
	
	@Override
	public void firePreUpdate(long modifiedId, String modifiedName, T updated, T old)
	{
		super.firePreUpdate(modifiedId, old.getName(), updated, old);
	}
	
	@Override
	public long getKeyForName(String id)
	{
		Long key = getNameMap().get(id);
		if (key == null)
			throw new IllegalArgumentException(name + " [" + id + "] does not exists");
		return key;
	}

	protected TransactionalMap<String, Long> getNameMap()
	{
		return HzTransactionTL.getHzContext().getMap(STORE_ID + "_names");
	}
}
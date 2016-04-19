/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.internal;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Default transactional runner - runs in a transaction which is consistent with the system configured storage engine.
 * <p>
 * The implementation is a bean however the object is lightweight and so can be also instantiated directly if needed.
 * @author K. Benedyczak
 */
@Component
@Primary
public class DefaultTransactionalRunner implements TransactionalRunner
{
	@Transactional
	@Override
	public void runInTransaction(TxRunnable code)
	{
		code.run();
	}

	@Transactional
	@Override
	public <T> T runInTransactionRet(TxRunnableRet<T> code)
	{
		return code.run();
	}
	
	@Transactional(autoCommit = false)
	@Override
	public void runInTransactionNoAutoCommit(TxRunnable code)
	{
		code.run();
	}
	
	@Transactional(autoCommit = false)
	@Override
	public <T> T runInTransactionNoAutoCommitRet(TxRunnableRet<T> code)
	{
		return code.run();
	}

}

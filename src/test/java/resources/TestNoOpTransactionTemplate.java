package resources;

import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class TestNoOpTransactionTemplate extends TransactionTemplate {

	public TestNoOpTransactionTemplate() {
		super(null);
	}

	@Override
	public <T> T execute(TransactionCallback<T> action) {
		return action.doInTransaction(new SimpleTransactionStatus());
	}

}

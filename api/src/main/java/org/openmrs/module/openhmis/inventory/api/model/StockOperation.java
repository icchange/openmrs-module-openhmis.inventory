package org.openmrs.module.openhmis.inventory.api.model;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.commons.api.entity.model.BaseCustomizableInstanceMetadata;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents an operation performed on item stock.  Examples of an operation include transferring
 * item stock from one stockroom to another or selling item stock to a patient.  Each of these types of operations is
 * modeled by a specific {@link IStockOperationType} class that defines what happens when a stock operation is
 * initially created (NEW), being processed (PENDING), cancelled (CANCELLED), or completed (COMPLETED).
 *
 * A stock operation is composed of the item stock changes and two types of stock transactions: reserved and completed.
 * The reserved transactions denote the state of the item stock while the operation is in progress (PENDING). For example,
 * when stock is transferred from one stockroom to another it is considered "owned" by the operation while the status is
 * PENDING and there will be the associated reserved transactions for each item stock while in that state. Once the operation
 * is CANCELLED or COMPLETED the pending transactions become completed transactions and the ownership of the item stock is
 * transitioned to the associated stockroom.
 */
public class StockOperation
		extends BaseCustomizableInstanceMetadata<IStockOperationType, StockOperationAttribute>
		implements Comparable<StockOperation> {
	public static final long serialVersionUID = 0L;

	private Integer id;
	private StockOperationStatus status;
	private Set<StockOperationItem> items;
	private Set<ReservedTransaction> reserved;
	private Set<StockOperationTransaction> transactions;

	private String operationNumber;
	private Date operationDate;
	protected Stockroom source;
	protected Stockroom destination;
	protected Patient patient;
	protected Institution institution;

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	public StockOperationStatus getStatus() {
		return status;
	}

	public void setStatus(StockOperationStatus status) {
		this.status = status;
	}

	public String getOperationNumber() {
		return operationNumber;
	}

	public void setOperationNumber(String operationNumber) {
		this.operationNumber = operationNumber;
	}

	public Date getOperationDate() {
		return operationDate;
	}

	public void setOperationDate(Date operationDate) {
		this.operationDate = operationDate;
	}

	public Stockroom getSource() {
		return source;
	}

	public void setSource(Stockroom newSource) {
		if (this.source == newSource) {
			return;
		}

		if (this.source != null && this.source != this.destination) {
			this.source.removeOperation(this);
		}

		// Update the source
		this.source = newSource;

		if (this.source != null) {
			this.source.addOperation(this);
		}
	}

	public Stockroom getDestination() {
		return destination;
	}

	public void setDestination(Stockroom newDestination) {
		if (this.destination == newDestination) {
			return;
		}

		if (this.destination != null && this.destination != this.source) {
			this.destination.removeOperation(this);
		}

		// Update the destination
		this.destination = newDestination;

		if (this.destination != null) {
			this.destination.addOperation(this);
		}
	}

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

	public StockOperationItem addItem(Item item, int quantity) {
		return addItem(item, quantity, null, null);
	}

	public StockOperationItem addItem(Item item, int quantity, Date expiration) {
		return addItem(item, quantity, expiration, null);
	}

	public StockOperationItem addItem(Item item, int quantity, Date expiration, StockOperation batchOperation) {
		if (item == null) {
			throw new IllegalArgumentException("The item must be defined");
		}

		StockOperationItem operationItem = new StockOperationItem();
		operationItem.setItem(item);
		operationItem.setQuantity(quantity);

		if (expiration == null) {
			if (item.getHasExpiration()) {
				operationItem.setCalculatedExpiration(true);
			} else {
				operationItem.setCalculatedExpiration(false);
			}
		} else {
			operationItem.setExpiration(expiration);
			operationItem.setCalculatedExpiration(false);
		}

		if (batchOperation == null) {
			operationItem.setCalculatedBatch(true);
		} else {
			operationItem.setBatchOperation(batchOperation);
			operationItem.setCalculatedBatch(false);
		}

		return addItem(operationItem);
	}

	public StockOperationItem addItem(StockOperationItem item) {
		if (item == null) {
			throw new IllegalArgumentException("This item to add must be defined.");
		}

		if (items == null) {
			items = new HashSet<StockOperationItem>();
		}

		item.setOperation(this);

		items.add(item);

		return item;
	}

	public void removeItem(StockOperationItem item) {
		if (item != null) {
			if (items == null) {
				return;
			}

			items.remove(item);
		}
	}

	public Set<StockOperationItem> getItems() {
		return items;
	}

	public void setItems(Set<StockOperationItem> items) {
		this.items = items;
	}

	public ReservedTransaction addReserved(Item item, int quantity) {
		return addReserved(item, quantity, null);
	}

	/**
	 * Adds a new reserved item.
	 * @param item The item to add.
	 * @param quantity The item quantity.
	 * @param expiration The item expiration or {@code null} if none.
	 * @return The newly created reserved item transaction.
	 */
	public ReservedTransaction addReserved(Item item, int quantity, Date expiration) {
		if (item == null) {
			throw new IllegalArgumentException("The item must be defined.");
		}

		ReservedTransaction tx = new ReservedTransaction();
		tx.setItem(item);
		tx.setQuantity(quantity);
		tx.setExpiration(expiration);
		tx.setCreator(Context.getAuthenticatedUser());
		tx.setDateCreated(new Date());

		return addReserved(tx);
	}

	/**
	 * Adds a new reserved item.
	 * @param tx The reserved item transaction to add.
	 * @return The saved reserved item transaction.
	 */
	public ReservedTransaction addReserved(ReservedTransaction tx) {
		if (tx == null) {
			throw new IllegalArgumentException("The transaction to add must be defined.");
		}

		if (reserved == null) {
			reserved = new HashSet<ReservedTransaction>();
		}

		tx.setOperation(this);
		tx.setAvailable(getInstanceType() != null ? getInstanceType().getAvailableWhenReserved() : false);

		reserved.add(tx);

		return tx;
	}

	public void removeReserved(ReservedTransaction tx) {
		if (tx != null) {
			if (reserved == null) {
				return;
			}

			reserved.remove(tx);
		}
	}

	public Set<ReservedTransaction> getReserved() {
		return reserved;
	}

	public void setReserved(Set<ReservedTransaction> reserved) {
		this.reserved = reserved;
	}

	public StockOperationTransaction addTransaction(StockOperationTransaction tx) {
		if (tx == null) {
			throw new IllegalArgumentException("The transaction to add must be defined.");
		}
		if (tx.getItem() == null) {
			throw new IllegalArgumentException("The transaction item must be defined.");
		}
		if (getInstanceType() == null) {
			throw new IllegalArgumentException("The operation type must be defined before adding transactions");
		}

		if (transactions == null) {
			transactions = new HashSet<StockOperationTransaction>();
		}

		tx.setOperation(this);

		transactions.add(tx);

		return tx;
	}

	public Set<StockOperationTransaction> getTransactions() {
		return transactions;
	}

	public void setTransactions(Set<StockOperationTransaction> transactions) {
		this.transactions = transactions;
	}
	
	public boolean hasReservedTransactions() {
		return (getReserved() != null && getReserved().size() > 0);
	}
	
	public boolean hasTransactions() {
		return (getTransactions() != null && getTransactions().size() > 0);
	}
	

	@Override
	public int compareTo(StockOperation o) {
		if (o == null) {
			return 1;
		}

		int result = 0;

		if (getId() != null && o.getId() != null) {
			result = getId().compareTo(o.getId());
		}

		if (result == 0) {
			result = getUuid().compareTo(o.getUuid());
		}

		return result;
	}
}


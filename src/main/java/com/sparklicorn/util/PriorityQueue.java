package com.sparklicorn.util;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

//Minheap
public class PriorityQueue<T> extends AbstractQueue<T> {

	private ArrayList<T> heap;
	private Comparator<T> comparator;

	public PriorityQueue(Comparator<T> comparator) {
		this.heap = new ArrayList<>();
		this.comparator = comparator;
	}

	private int getLeft(int i) {
		return (2 * i) + 1;
	}

	private int getRight(int i) {
		return 2 * (i + 1);
	}

	private int getParent(int i) {
		return (i - 1) / 2;
	}

	private boolean hasParent(int i) {
		return i > 0;
	}

	private boolean hasLeft(int i) {
		return (i >= 0 && getLeft(i) < heap.size());
	}

	private boolean hasRight(int i) {
		return (i >= 0 && getRight(i) < heap.size());
	}

	private int getLesserChildIndex(int i) {
		int result = -1;
		if (hasRight(i)) {
			int right = getRight(i);
			int left = getLeft(i);
			result = (comparator.compare(heap.get(right), heap.get(left)) < 0) ? right : left;
		} else if (hasLeft(i)) {
			result = getLeft(i);
		}
		return result;
	}

	private boolean propogateDown(int index) {
		boolean result = false;
		boolean stillPropogating = true;
		T element = heap.get(index);
		while (stillPropogating) {
			stillPropogating = false;
			int childIndex = getLesserChildIndex(index);
			if (childIndex > -1) {
				T child = heap.get(childIndex);
				if (comparator.compare(child, element) < 0) {
					heap.set(index, child);
					heap.set(childIndex, element);
					index = childIndex;
					stillPropogating = true;
					result = true;
				}
			}
		}
		return result;
	}

	private boolean propogateUp(int index) {
		boolean result = false;

		T element = heap.get(index);
		int parentIndex = getParent(index);
		T parent = heap.get(parentIndex);

		while (hasParent(index) && comparator.compare(element, parent) < 0) {
			heap.set(parentIndex, element);
			heap.set(index, parent);
			index = parentIndex;
			parentIndex = getParent(index);
			parent = heap.get(parentIndex);
			result = true;
		}

		return result;
	}

	@Override public boolean offer(T e) {
		heap.add(e);
		propogateUp(heap.size() - 1);
		return true;
	}

	@Override public T poll() {
		T result = null;
		if (!heap.isEmpty()) {
			result = heap.get(0);
			heap.set(0, heap.get(heap.size() - 1));
			heap.remove(heap.size() - 1);
			if (heap.size() > 1) {
				propogateDown(0);
			}
		}
		return result;
	}

	@Override public T peek() {
		return heap.get(0);
	}

	@Override public Iterator<T> iterator() {
		return heap.iterator();
	}

	@Override public int size() {
		return heap.size();
	}
}

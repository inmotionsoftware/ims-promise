package com.inmotionsoftware.tuple;

import java.util.Collection;
import java.util.Iterator;

public abstract class Tuple {
	public class TupleIterator implements Iterator<Object> {
		private Tuple m_Parent;
		private int m_Idx = 0;
		
		protected TupleIterator(Tuple t) {
			m_Parent = t;
		}

		@Override
		public boolean hasNext() {
			return m_Idx < m_Parent.getSize();
		}

		@Override
		public Object next() {
			return m_Parent.get(m_Idx);
		}
	};

	public abstract int getSize();
	public abstract int indexOf(Object o);
	public abstract int lastIndexOf(Object o);
	public abstract Object[] toArray();
	public abstract Collection<Object> toCollection();
	public abstract int hashCode();
	public abstract boolean equals(Object o);
	public abstract boolean contains(Object o);	
	public abstract Object get(int i) throws IndexOutOfBoundsException;
	
	public Iterator<Object> iterator() {
		return new TupleIterator(this);
	}
	
	public static <A> Unary<A> make(A a) {
		return new Unary<A>(a);
	}

	public static <A,B> Pair<A,B> make(A a, B b) {
		return new Pair<A,B>(a,b);
	}
	
	public static <A,B,C> Triplet<A,B,C> make(A a, B b, C c) {
		return new Triplet<A,B,C>(a,b,c);
	}
	
	public static <A,B,C,D> Quartet<A,B,C,D> make(A a, B b, C c, D d) {
		return new Quartet<A,B,C,D>(a,b,c,d);
	}
	
	public static <A,B,C,D,E> Quintet<A,B,C,D,E> make(A a, B b, C c, D d, E e) {
		return new Quintet<A,B,C,D,E>(a,b,c,d,e);
	}
	
	public static Tuple make(Object...objects) throws IndexOutOfBoundsException {

		switch(objects.length) {
		case 1:
			return new Unary<Object>(objects[0]);
		case 2:
			return new Pair<Object,Object>(objects[0], objects[1]);
		case 3:
			return new Triplet<Object,Object,Object>(objects[0], objects[1], objects[2]);
		case 4:
			return new Quartet<Object,Object,Object,Object>(objects[0], objects[1], objects[2], objects[3]);
		case 5:
			return new Quintet<Object,Object,Object,Object,Object>(objects[0], objects[1], objects[2], objects[3], objects[4]);
		default:
			throw new IndexOutOfBoundsException();
		}
	}
	
	protected Tuple() {}
}

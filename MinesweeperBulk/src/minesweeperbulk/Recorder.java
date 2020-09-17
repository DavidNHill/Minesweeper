package minesweeperbulk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Recorder {
	
	public class Value implements Comparable<Value> {
		
		public int key;
		public int played;
		public int won;
		public int lost;
		
		@Override
		public int compareTo(Value o) {
			return this.key - o.key;
		} 
		
	}
	
	private Map<Integer, Value> store = new HashMap<>();
	
	
	public Recorder() {
		
	}
	
	public void add(int key, boolean won) {
		
		Value value;
		if (store.containsKey(key)) {
			value = store.get(key);
		} else {
			value = new Value();
			value.key = key;
			store.put(key, value);
		}

		value.played++;
		if (won) {
			value.won++;
		} else {
			value.lost++;
		}
		
	}
	
	public List<Value> getValues() {
		
		List<Value> result = new ArrayList<>(store.values());
		
		Collections.sort(result);
		
		return result;
		
	}

}

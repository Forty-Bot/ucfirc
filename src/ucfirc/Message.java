package ucfirc;

public class Message {

	public enum Type {
		ERROR(0),
		SAY(1),
		CHANNEL(2),
		ACTION(3);

		public final int value;
		
		private Type(int value) {
			this.value = value;
		}

		private static final Type[] _values = Type.values();
		public static Type fromInt(int i) {
			return _values[i];
		}
	}

	public String usr;
	public String txt;
	public Object src;
	public Type type;

	public Message(String usr, String txt, Object src, Type type) {
		this.usr = usr;
		this.txt = txt;
		this.src = src;
		this.type = type;
	}

	public String toString() {
		
		switch(type) {
		case ERROR:
			return txt;
		case SAY:
			return "[" + usr + "] " + txt;
		case CHANNEL:
			return "*" + usr + " " + txt;
		case ACTION:
			return "***" + usr + " " + txt;
		}
		throw new Error(new IllegalArgumentException(type.toString()));

	}

}

package org.df4j.codec.javon.parser;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.util.List;
import org.df4j.pipeline.codec.javon.builder.impl.JavonBuilder;
import org.df4j.pipeline.codec.javon.builder.impl.JavonPrinter;
import org.df4j.pipeline.codec.javon.parser.JavonParser;
import org.junit.Test;

public class so1688099test {
	protected static final String inp=
"Data("+
    "title:\"ComputingandInformationsystems\","+
    "id:1,"+
    "children:true,"+
    "groups:[Data("+
        "title:\"LeveloneCIS\","+
        "id:2,"+
        "children:true,"+
        "groups:[Data("+
            "title:\"IntroToComputingandInternet\","+
            "id:3,"+
            "children:false,"+
            "groups:[]"+
        ")]"+
    ")]"+
")";			
	
	@Test
    public void testWithPrinter() throws IOException, Exception {
        JavonPrinter pr = new JavonPrinter();
        JavonParser mp=new JavonParser(pr);
        Object obj = mp.parseFrom(inp);
		String res = obj.toString();
		compareStrings(inp, res);
        assertEquals(inp, res);
    }

	@Test
    public void testWithBuilder() throws IOException, Exception {
        JavonBuilder bd = new JavonBuilder();
        JavonParser mp=new JavonParser(bd);
        bd.put("Data", Data.class);
        Object obj = mp.parseFrom(inp);
		String res = obj.toString();
        compareStrings(inp, res);
        assertEquals(inp, res);
	}
	
	protected void compareStrings(String exp, String act) {
		final int actLength = act.length();
		final int expLength = exp.length();
		if (expLength!=actLength) {
			System.out.println("exp:"+expLength+" but act.length="+actLength);
		}
		int len=Math.min(actLength, expLength);
		for (int k=0; k<len; k++) {
			if (act.charAt(k)!=exp.charAt(k)) {
				System.out.println(exp.substring(k));
				System.out.println(act.substring(k));
				break;
			}
		}
	}

	public static class Data {
		private String title;
		private int id;
		private Boolean children;
		private List<Data> groups;

		public String getTitle() {
			return title;
		}

		public int getId() {
			return id;
		}

		public Boolean getChildren() {
			return children;
		}

		public List<Data> getGroups() {
			return groups;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public void setId(int id) {
			this.id = id;
		}

		public void setChildren(Boolean children) {
			this.children = children;
		}

		public void setGroups(List<Data> groups) {
			this.groups = groups;
		}

		public String toString() {
			return String.format("Data(title:\"%s\",id:%d,children:%s,groups:%s)", title,
					id, children, groups);
		}
	}
}

package org.master.feature.tabs;

public  class TabItem{

        public String title;
        public int  order;
        public int id;
        public int icon_in_pos;
        public int icon_out_pos;
        public boolean enabled;

        private static int dialogFilterPointer = 100;
        public int localId = dialogFilterPointer++;


        public TabItem(String title, int order, int id, int icon_in, int icon_out,boolean enabled) {
            this.title = title;
            this.order = order;
            this.id = id;
            this.icon_in_pos = icon_in;
            this.icon_out_pos = icon_out;
            this.enabled = enabled;

        }

        public static TabItem create(String title, int order, int id, int icon_in, int icon_out, boolean enabled){
            return  new TabItem(title, order,  id,  icon_in,  icon_out,enabled);
        }
    }

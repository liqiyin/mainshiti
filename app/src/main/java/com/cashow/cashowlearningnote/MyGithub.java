package com.cashow.cashowlearningnote;

import br.tiagohm.markdownview.css.styles.Github;

public class MyGithub extends Github {
    public MyGithub() {
        super();
        this.addRule("body", new String[]{"line-height: 1.6", "padding: 15px"});
    }
}

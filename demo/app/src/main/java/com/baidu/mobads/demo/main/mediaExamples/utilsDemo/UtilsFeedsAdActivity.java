package com.baidu.mobads.demo.main.mediaExamples.utilsDemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.baidu.mobads.demo.main.R;

public class UtilsFeedsAdActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_utils_demo);
        Button finishButton = (Button) this.findViewById(R.id.feed_utils_finish);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UtilsFeedsAdActivity.this, FinishAdActivity.class);
                startActivity(intent);
            }
        });
        Button clearButton = (Button) this.findViewById(R.id.feed_utils_clear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UtilsFeedsAdActivity.this, ClearAdActivity.class);
                startActivity(intent);
            }
        });

    }
}

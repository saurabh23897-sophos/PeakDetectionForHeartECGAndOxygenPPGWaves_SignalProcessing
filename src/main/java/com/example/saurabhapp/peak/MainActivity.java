package com.example.saurabhapp.peak;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Objects;


public class MainActivity extends Activity {

    double x[];
    int pp[];
    EditText editText;
    TextView hr;
    Button b, bf;
    Boolean loaded = false;
    int fs = 0, index, pindex;
    LineGraphSeries<DataPoint> series;
    PointsGraphSeries<DataPoint> pseries;
    GraphView graph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        b = (Button) findViewById(R.id.button);
        bf = (Button) findViewById(R.id.button2);
        hr = (TextView) findViewById(R.id.hr);
        editText = (EditText) findViewById(R.id.editText);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fs = Integer.parseInt(editText.getText().toString());
                if(fs == 0) {
                    Toast.makeText(getBaseContext(), "First Enter Sampling Frequency", Toast.LENGTH_SHORT).show();
                } else {
                    graph.removeAllSeries();
                    load();
                }
            }
        });

        bf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(loaded) {
                    bf.setClickable(false);
                    index = 0;
                    pindex = 0;
                    pseries = new PointsGraphSeries<>();
                    pseries.setColor(Color.RED);
                    pseries.setSize(7);
                    series = new LineGraphSeries<>();
                    series.setDrawDataPoints(true);
                    series.setDataPointsRadius(2);
                    series.setThickness(4);
                    graph.addSeries(series);
                    graph.addSeries(pseries);
                    findPeak();
                }
            }
        });

        graph = (GraphView) findViewById(R.id.graph);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollableY(true);
        graph.getViewport().setMaxY(1);
        graph.getViewport().setMinY(-1);
        graph.getViewport().setMaxX(1000);
        graph.getViewport().setMinX(0);

    }

    public void check() {
        if(loaded) {
            hr.setText(String.format(Locale.US, "%d", 0));
            Toast.makeText(getBaseContext(), "Signal loaded.", Toast.LENGTH_SHORT).show();
            bf.setClickable(true);
        } else {
            Toast.makeText(getBaseContext(), "Signal not loaded.", Toast.LENGTH_SHORT).show();
            bf.setClickable(false);
        }
    }

    public void load() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("file/*");
            startActivityForResult(intent, 1);
        } catch (NullPointerException ex) {
            Toast.makeText(getBaseContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void findPeak() {
        double max = 0;
        for(double i : x)
            max = Math.max(max, Math.abs(i));
        for(int i = 0; i < x.length; i++)
            x[i] /= max;

        int l = (int) (0.3 * 125);
        int ag = (int) (0.03 * 125);
        double a = (double) (l / (2 * ag));

        double g[] = new double[l + 1];
        for(int i = 0; i <= l; i++)
            g[i] = Math.exp(-0.5 * Math.pow(((i - l / 2) / a), 2));

        double h[] = new double[l];
        for(int i = 0; i < l; i++)
            h[i] = g[i + 1] - g[i];

        double xn[] = new double[x.length + h.length - 1];
        for(int i = 0; i < xn.length; i++) {
            xn[i] = 0;
            if(i >= h.length - 1)
                xn[i] = x[i - h.length + 1];
        }

        max = 0;
        double x1[] = new double[x.length];
        for(int i = 0; i < x.length; i++) {
            x1[i] = 0;
            for(int j = 0; j < h.length; j++)
                x1[i] += xn[j + i] * h[h.length - j - 1];
            max = Math.max(max, Math.abs(x1[i]));
        }
        for(int i = 0; i < x1.length; i++)
            x1[i] /= max;

        double mean = 0, std = 0;
        double en[] = new double[x1.length];
        for(int i = 0; i < en.length; i++) {
            en[i] = Math.pow(x1[i], 2);
            mean += en[i];
        }
        mean /= en.length;

        for(double i : en)
            std += Math.pow(i - mean, 2);
        std = Math.sqrt(std / (en.length - 1));

        for(int i = 0; i < en.length; i++)
            if(en[i] < std)
                en[i] = 0.000000000000000000001;

        double s[] = new double[en.length];
        for(int i = 0; i < s.length; i++)
            s[i] = - Math.pow(en[i], 2) * Math.log(Math.pow(en[i], 2));

        int div = 40;
        double s1[] = new double[s.length + div - 1];
        for(int i = 0; i < s1.length; i++) {
            s1[i] = 0;
            if(i >= div - 1)
                s1[i] = s[i - div + 1];
        }

        max = 0;
        for(int i = 0; i < s.length; i++) {
            s[i] = 0;
            for(int j = 0; j < div; j++)
                s[i] += s1[i + j];
            s[i] /= div;
            max = Math.max(max, Math.abs(s[i]));
        }
        for(int i = 0; i < s.length; i++)
            s[i] /= max;

        double s2[] = new double[s.length + h.length - 1];
        for(int i = 0; i < s2.length; i++) {
            s2[i] = 0;
            if(i >= h.length - 1)
                s2[i] = s[i - h.length + 1];
        }

        max = 0;
        double d[] = new double[x.length];
        for(int i = 0; i < x.length; i++) {
            d[i] = 0;
            for(int j = 0; j < h.length; j++)
                d[i] += s2[j + i] * h[h.length - j - 1];
            max = Math.max(max, Math.abs(d[i]));
        }
        for(int i = 0; i < d.length; i++)
            d[i] /= max;

        int nz = 0;
        for(int i = 1; i < d.length; i++)
            if(d[i] * d[i - 1] < 0 && d[i] < 0)
                ++nz;

        int k = 1;
        int z[] = new int[nz + 2];
        z[0] = 0;
        for(int i = 1; i < d.length; i++) {
            if(d[i] * d[i - 1] < 0 && d[i] < 0) {
                z[k] = i;
                ++k;
            }
        }
        z[k] = d.length - 1;

        int w = 40 * (fs / 125);
        int p[] = new int[z.length];
        for(int b = 0; b < 5; b++) {
            for(int i = 0; i < z.length; i++) {
                if(z[i] > w && z[i] < x.length - w - 1) {
                    max = 0;
                    for(int j = 0; j <= 2 * w; j++) {
                        if(x[z[i] - w + j] > max) {
                            max = x[z[i] - w + j];
                            p[i] = z[i] - w + j;
                        }
                    }
                } else if(z[i] > w) {
                    max = 0;
                    for(int j = 0; j <= x.length - z[i] - 1 + w; j++) {
                        if(x[z[i] - w + j] > max) {
                            max = x[z[i] - w + j];
                            p[i] = z[i] - w + j;
                        }
                    }
                } else {
                    max = 0;
                    for(int j = 0; j < z[i] + w; j++) {
                        if(x[j] > max) {
                            max = x[j];
                            p[i] = j;
                        }
                    }
                }
            }
            System.arraycopy(p, 0, z, 0, z.length);
        }

        int p1[] = new int[p.length];
        k = 0;
        p1[k] = p[0];
        for(int i = 1; i < p.length; i++) {
            if(p[i] != p[i - 1]) {
                ++k;
                p1[k] = p[i];
            }
        }

        pp = new int[k + 1];
        System.arraycopy(p1, 0, pp, 0, pp.length);

        int min = x.length / (fs * 60);
        hr.setText(String.format(Locale.US, "%d", pp.length / min));

        mean = 0;
        for(double i : x)
            mean += i;
        mean /= x.length;

        max = 0;
        for(int i = 0; i < x.length; i++) {
            x[i] -= mean;
            max = Math.max(max, Math.abs(x[i]));
        }
        for(int i = 0; i < x.length; i++)
            x[i] /= max;

        new Thread(new Runnable() {
            @Override
            public void run() {
                for(double i : x) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addData();
                        }
                    });
                    try {
                        Thread.sleep(1);
                    } catch (Exception e) {
                        Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }).start();
    }

    public void addData() {
        if(index < x.length) {
            if(pindex < pp.length) {
                if(index == pp[pindex]) {
                    pseries.appendData(new DataPoint(index, x[index]), true, x.length);
                    ++pindex;
                }
            }
            series.appendData(new DataPoint(index, x[index]), true, x.length);
            ++index;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                try {
                    int length = 0;
                    File myFile = new File(Objects.requireNonNull(data.getData()).getPath());
                    FileInputStream fIn = new FileInputStream(myFile);
                    BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));
                    while(myReader.readLine() != null)
                        ++length;
                    myReader.close();
                    length -= (length % fs);
                    File file = new File(data.getData().getPath());
                    FileInputStream fileInputStream = new FileInputStream(file);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
                    x = new double[length];
                    for(int i = 0; i < length; i++)
                        x[i] = Double.parseDouble(bufferedReader.readLine());
                    bufferedReader.close();
                    loaded = true;
                } catch (Exception e) {
                    Toast.makeText(getBaseContext(), "Error: File not loaded.", Toast.LENGTH_SHORT).show();
                    loaded = false;
                }
            } else  {
                Toast.makeText(getBaseContext(), "Error: File not opened.", Toast.LENGTH_SHORT).show();
                loaded = false;
            }
        }
        check();
    }
}
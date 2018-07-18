package com.lafaya.toolbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * Created by JeffYoung on 2016/11/3.
 **/
public class LoginActivity extends Activity{
    ImageButton button_login_jump;

    Button button_forgotuser, button_register,button_login;
    EditText edittext_loginpassword, edittext_loginusername;
    CheckBox checkbos_recorduser;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginlayout);
        button_login_jump = (ImageButton)findViewById(R.id.button_login_jump);

        button_login = (Button)findViewById(R.id.button_login);
        button_register = (Button)findViewById(R.id.button_register);
        button_forgotuser = (Button)findViewById(R.id.button_forgotuser);

        edittext_loginusername = (EditText)findViewById(R.id.edittext_loginusername);
        edittext_loginpassword = (EditText)findViewById(R.id.edittext_loginpassword);
        checkbos_recorduser = (CheckBox)findViewById(R.id.checkbos_recorduser);

        button_login_jump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                startActivity(intent);
                LoginActivity.this.finish();
            }
        });


        button_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        button_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });
    }


    private void login(){
        String username = edittext_loginusername.getText().toString();
        String password = edittext_loginpassword.getText().toString();

        if(!username.equals("lafaya") || (!password.equals("lafaya8888"))){
            //用户名为空，请正确输入用户名
            Toast.makeText(this,"用户名或密码错误，请重新输入！",Toast.LENGTH_LONG).show();

        }else{
            Intent intent = new Intent(LoginActivity.this,MainActivity.class);
            startActivity(intent);
            LoginActivity.this.finish();
        }
    }

    private void register(){
        Toast.makeText(this,"暂时不支持注册，敬请期待！",Toast.LENGTH_LONG).show();
    }
}

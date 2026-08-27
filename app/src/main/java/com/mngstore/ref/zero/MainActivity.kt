package com.mngstore.ref.zero

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.NumberFormat
import java.util.Locale

/**
 * نمونه فارسی و آفلاین مدیریت مالی شخصی/کسب‌وکار.
 * چهار نوع تراکنش درآمد، هزینه، طلب و بدهی را نگهداری می‌کند و خلاصه مالی می‌سازد.
 */
class MainActivity : Activity() {
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var root: LinearLayout
    private val rows = mutableListOf<Tx>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs=getSharedPreferences("zero_fa",MODE_PRIVATE)
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setBackgroundColor(Color.rgb(247,248,250))}
        window.decorView.layoutDirection=View.LAYOUT_DIRECTION_RTL
        setContentView(root)
        load(); showHome()
    }

    /** داده‌ها در SharedPreferences به شکل ساده ذخیره می‌شوند تا نمونه کاملاً آفلاین باشد. */
    private fun load(){
        val raw=prefs.getString("rows","").orEmpty()
        if(raw.isBlank()){
            rows += Tx("درآمد",1_500_000,"فروش امروز")
            rows += Tx("هزینه",320_000,"خرید مواد اولیه")
            rows += Tx("طلب",750_000,"مانده حساب مشتری")
            save()
        } else raw.split("\n").filter{it.isNotBlank()}.forEach{p-> val a=p.split("|",limit=3); if(a.size==3) rows+=Tx(a[0],a[1].toLongOrNull()?:0,a[2]) }
    }
    private fun save(){ prefs.edit().putString("rows",rows.joinToString("\n"){"${it.type}|${it.amount}|${it.note.replace("|","-")}"}).apply() }

    /** داشبورد خلاصه مالی. */
    private fun showHome(){
        root.removeAllViews(); header("مدیریت مالی کامل","نمونه مرجع فارسی و آفلاین")
        val income=rows.filter{it.type=="درآمد"}.sumOf{it.amount}; val expense=rows.filter{it.type=="هزینه"}.sumOf{it.amount}; val receivable=rows.filter{it.type=="طلب"}.sumOf{it.amount}; val debt=rows.filter{it.type=="بدهی"}.sumOf{it.amount}
        root.addView(card("درآمد: ${money(income)}\nهزینه: ${money(expense)}\nخالص: ${money(income-expense)}\nمطالبات: ${money(receivable)}\nبدهی‌ها: ${money(debt)}"))
        root.addView(button("+ ثبت تراکنش"){dialog()})
        root.addView(title("تراکنش‌ها"))
        rows.asReversed().forEach{ root.addView(card("${it.type} — ${money(it.amount)}\n${it.note}")) }
    }

    /** فرم ثبت تراکنش جدید با انتخاب نوع. */
    private fun dialog(){
        val types=arrayOf("درآمد","هزینه","طلب","بدهی"); val spinner=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,types)}
        val amount=EditText(this).apply{hint="مبلغ به تومان";inputType=InputType.TYPE_CLASS_NUMBER;gravity=Gravity.END}
        val note=EditText(this).apply{hint="توضیح";gravity=Gravity.END}
        val form=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(dp(18),dp(8),dp(18),0);addView(spinner);addView(amount);addView(note)}
        AlertDialog.Builder(this).setTitle("تراکنش جدید").setView(form).setNegativeButton("انصراف",null).setPositiveButton("ثبت"){_,_->
            val value=normalize(amount.text.toString()).toLongOrNull()?:0
            if(value<=0) toast("مبلغ معتبر وارد کنید") else { rows+=Tx(types[spinner.selectedItemPosition],value,note.text.toString()); save(); showHome() }
        }.show()
    }

    private fun header(t:String,s:String){root.addView(LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(20),dp(20),dp(16));setBackgroundColor(Color.rgb(43,92,76));addView(TextView(this@MainActivity).apply{text=t;textSize=24f;setTextColor(Color.WHITE);gravity=Gravity.END});addView(TextView(this@MainActivity).apply{text=s;textSize=13f;setTextColor(Color.LTGRAY);gravity=Gravity.END})})}
    private fun button(t:String,a:()->Unit)=Button(this).apply{text=t;isAllCaps=false;setOnClickListener{a()};layoutParams=LinearLayout.LayoutParams(-1,dp(54)).apply{setMargins(dp(16),dp(6),dp(16),dp(6))}}
    private fun title(t:String)=TextView(this).apply{text=t;textSize=18f;gravity=Gravity.END;setPadding(dp(18),dp(18),dp(18),dp(6))}
    private fun card(t:String)=TextView(this).apply{text=t;textSize=16f;gravity=Gravity.END;setPadding(dp(18),dp(16),dp(18),dp(16));setBackgroundColor(Color.WHITE);layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(16),dp(5),dp(16),dp(5))}}
    /** تبدیل مبلغ به نمایش سه‌رقمی فارسی. */
    private fun money(v:Long)=fa(NumberFormat.getIntegerInstance(Locale.US).format(v).replace(",","٫"))+" تومان"
    private fun normalize(v:String)=v.replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4').replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9').replace("٫","").replace(",","").replace(" ","")
    private fun fa(v:String)=v.replace('0','۰').replace('1','۱').replace('2','۲').replace('3','۳').replace('4','۴').replace('5','۵').replace('6','۶').replace('7','۷').replace('8','۸').replace('9','۹')
    private fun toast(v:String)=Toast.makeText(this,v,Toast.LENGTH_SHORT).show()
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}

data class Tx(val type:String,val amount:Long,val note:String)

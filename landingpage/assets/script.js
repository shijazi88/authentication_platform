const toggle=document.getElementById('langToggle');
function setLang(lang){
  document.documentElement.lang=lang;
  document.body.dir=lang==='ar'?'rtl':'ltr';
  toggle.textContent=lang==='ar'?'English':'العربية';
  document.querySelectorAll('[data-en]').forEach(el=>{el.textContent=el.dataset[lang];});
  document.querySelectorAll('[data-placeholder-en]').forEach(el=>{el.placeholder=el.dataset[`placeholder${lang==='ar'?'-ar':'-en'}`]||el.placeholder;});
}
toggle.addEventListener('click',()=>setLang(document.documentElement.lang==='ar'?'en':'ar'));
const io=new IntersectionObserver(entries=>entries.forEach(e=>{if(e.isIntersecting)e.target.classList.add('visible')}),{threshold:.12});
document.querySelectorAll('.reveal').forEach(el=>io.observe(el));
setLang('en');

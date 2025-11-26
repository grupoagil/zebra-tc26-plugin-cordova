# cordova-plugin-datawedge

Plugin Cordova simples para integrar leitor de código de barras do Zebra (DataWedge) com apps Android.

## Instalação

```bash
cordova plugin add ./cordova-plugin-datawedge
# ou
cordova plugin add https://seu-repo-git/cordova-plugin-datawedge.git
```

## Uso

```javascript
document.addEventListener('deviceready', function () {
    DataWedge.startListening(function (result) {
        console.log('Código lido:', result.barcode);
        // exemplo: preencher input
        var input = document.getElementById('codigo_barra');
        if (input) {
            input.value = result.barcode;
        }
    }, function (err) {
        console.error('Erro DataWedge:', err);
    });
});
```

### Configuração no DataWedge (Zebra TC26)

- Crie um Profile e associe seu app (com.agildesenvolvimento.coletor)
- Em **Barcode Input**: habilite
- Em **Intent Output**:
  - Enable: ON
  - Intent action: `com.agil.ACTION`
  - Intent delivery: Broadcast

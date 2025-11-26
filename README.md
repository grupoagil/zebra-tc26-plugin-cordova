# cordova-plugin-datawedge-auto

Plugin Cordova para integrar leitores Zebra (ex.: TC26) via DataWedge, com:

- Criação automática de Profile no DataWedge
- Configuração automática de Intent Output
- Associação automática ao app `com.agildesenvolvimento.coletor`
- Recebimento das leituras via JavaScript

## Premissas do plugin

- Package / applicationId do app: **com.agildesenvolvimento.coletor**
- Intent Action: **com.agil.ACTION**
- Nome do Profile criado no DataWedge: **ColetorAgil**

## Instalação

Copie a pasta para o seu projeto e instale:

```bash
cordova plugin add ./cordova-plugin-datawedge-auto
```

## Uso no JavaScript

```javascript
document.addEventListener('deviceready', function () {
    // (opcional) força recriação/atualização do profile
    DataWedge.createOrUpdateProfile(function(msg){
        console.log('Profile OK:', msg);
    }, function(err){
        console.error('Erro profile:', err);
    });

    // Inicia listener para receber os scans
    DataWedge.startListening(function(result) {
        console.log('Código lido:', result.barcode);

        var input = document.getElementById('codigo_barra');
        if (input) {
            input.value = result.barcode;
        }
    }, function(err) {
        console.error('Erro DataWedge:', err);
    });
});
```

## O que o plugin faz no DataWedge

- Cria/atualiza o profile **ColetorAgil**
- Habilita o plugin **BARCODE**
- Habilita o plugin **INTENT** com:
  - `intent_output_enabled = true`
  - `intent_action = com.agil.ACTION`
  - `intent_delivery = 2` (broadcast)
- Associa o profile ao app `com.agildesenvolvimento.coletor` em todas as activities.

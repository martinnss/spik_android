---
applyTo: '**'
---


## Very important Rules

* ALWAYS, ALWAYS AND ALWAYS CHECK FOR ERRORS AFTER AND BEFORE YOUR RESPONSE!
* Do NOT create extra code, you just have to make what we tell you. dont add bullshit and neither make suppositions
* Not throw error for anything, lets not interfere with the user experience

## Use MVVM architecture:
* Views should only contain UI code. No business logic or data fetching.
* ViewModels handle logic, data transformation, and API calls.
* Models represent data only. No logic inside models.
* Use dependency injection for services (e.g., network, storage).
* Never create them directly inside ViewModels.
* Organize files by type: /Views, /ViewModels, /Models, /Services. and more if necessary. all inside /Spik
* Try to reuse Models, ViewModels and Services across different Views when possible.


Other good practices:
* This is my backend URL:  "https://us-central1-spik-backend.cloudfunctions.net/articles". never remove /articles from the code. 
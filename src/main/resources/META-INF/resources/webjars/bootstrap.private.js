(function () {
    if (!$('.menu-main .plugin-id-menu').length) {
        $('.menu-main').append('<li><a class="plugin-id-menu" href="#/id">{{menu.id}}</a></li>');
        $('.menu-user').append('<li><a href="#/id/user">{{menu.profile}}</a></li>');
    }
})();

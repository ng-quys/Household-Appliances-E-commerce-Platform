document.addEventListener('DOMContentLoaded', () => {
  const wrapper = document.querySelector('.category-menu-wrapper');
  const toggle = document.querySelector('[data-category-toggle]');
  const menu = document.querySelector('[data-category-menu]');
  const tabs = Array.from(document.querySelectorAll('[data-genre-tab]'));
  const panels = Array.from(document.querySelectorAll('[data-genre-panel]'));

  if (!wrapper || !toggle || !menu) {
    return;
  }

  let closeTimer = null;

  const setActiveGenre = (genreId) => {
    tabs.forEach((tab) => {
      tab.classList.toggle('is-active', tab.dataset.genreTab === genreId);
    });

    panels.forEach((panel) => {
      panel.classList.toggle('is-active', panel.dataset.genrePanel === genreId);
    });
  };

  const openMenu = () => {
    if (closeTimer) {
      window.clearTimeout(closeTimer);
      closeTimer = null;
    }
    menu.classList.add('is-open');
    toggle.setAttribute('aria-expanded', 'true');
  };

  const closeMenu = () => {
    menu.classList.remove('is-open');
    toggle.setAttribute('aria-expanded', 'false');
  };

  const scheduleClose = () => {
    if (closeTimer) {
      window.clearTimeout(closeTimer);
    }
    closeTimer = window.setTimeout(() => {
      closeMenu();
    }, 140);
  };

  if (tabs.length > 0) {
    const initialTab = tabs.find((tab) => tab.classList.contains('is-active')) || tabs[0];
    if (initialTab?.dataset.genreTab) {
      setActiveGenre(initialTab.dataset.genreTab);
    }
  }

  toggle.addEventListener('click', (event) => {
    event.preventDefault();
    event.stopPropagation();

    if (menu.classList.contains('is-open')) {
      closeMenu();
      return;
    }

    openMenu();
  });

  wrapper.addEventListener('mouseenter', openMenu);
  wrapper.addEventListener('mouseleave', scheduleClose);

  menu.addEventListener('click', (event) => {
    event.stopPropagation();
  });

  tabs.forEach((tab) => {
    tab.addEventListener('mouseenter', () => {
      if (tab.dataset.genreTab) {
        setActiveGenre(tab.dataset.genreTab);
      }
    });

    tab.addEventListener('focus', () => {
      if (tab.dataset.genreTab) {
        setActiveGenre(tab.dataset.genreTab);
      }
    });
  });

  document.addEventListener('click', () => {
    closeMenu();
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      closeMenu();
    }
  });
});

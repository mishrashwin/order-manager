/**
 * Phone input widget with country code selector.
 * Combines a <select> (country code) + <input> (local number) into a single E.164-formatted
 * hidden field that is submitted with the form.
 *
 * Usage:
 *   initPhoneInput('hiddenFieldId', 'countrySelectId', 'localNumberInputId')
 *
 * The hidden field stores values in E.164 format: +{countryCode}{localNumber}
 * e.g. +919876543210
 */

var PHONE_COUNTRIES = [
  { code: '+91',  label: '\uD83C\uDDEE\uD83C\uDDF3 India (+91)',          maxLen: 10 },
  { code: '+1',   label: '\uD83C\uDDFA\uD83C\uDDF8 USA / Canada (+1)',    maxLen: 10 },
  { code: '+44',  label: '\uD83C\uDDEC\uD83C\uDDE7 UK (+44)',             maxLen: 10 },
  { code: '+61',  label: '\uD83C\uDDE6\uD83C\uDDFA Australia (+61)',      maxLen: 9  },
  { code: '+971', label: '\uD83C\uDDE6\uD83C\uDDEA UAE (+971)',           maxLen: 9  },
  { code: '+65',  label: '\uD83C\uDDF8\uD83C\uDDEC Singapore (+65)',      maxLen: 8  },
  { code: '+60',  label: '\uD83C\uDDF2\uD83C\uDDFE Malaysia (+60)',       maxLen: 9  },
  { code: '+49',  label: '\uD83C\uDDE9\uD83C\uDDEA Germany (+49)',        maxLen: 11 },
  { code: '+33',  label: '\uD83C\uDDEB\uD83C\uDDF7 France (+33)',         maxLen: 9  },
  { code: '+86',  label: '\uD83C\uDDE8\uD83C\uDDF3 China (+86)',          maxLen: 11 },
  { code: '+81',  label: '\uD83C\uDDEF\uD83C\uDDF5 Japan (+81)',          maxLen: 10 },
  { code: '+7',   label: '\uD83C\uDDF7\uD83C\uDDFA Russia (+7)',          maxLen: 10 },
  { code: '+55',  label: '\uD83C\uDDE7\uD83C\uDDF7 Brazil (+55)',         maxLen: 11 },
  { code: '+27',  label: '\uD83C\uDDFF\uD83C\uDDE6 South Africa (+27)',   maxLen: 9  },
  { code: '+92',  label: '\uD83C\uDDF5\uD83C\uDDF0 Pakistan (+92)',       maxLen: 10 },
  { code: '+880', label: '\uD83C\uDDE7\uD83C\uDDE9 Bangladesh (+880)',    maxLen: 10 },
  { code: '+94',  label: '\uD83C\uDDF1\uD83C\uDDF0 Sri Lanka (+94)',      maxLen: 9  },
  { code: '+977', label: '\uD83C\uDDF3\uD83C\uDDF5 Nepal (+977)',         maxLen: 10 },
  { code: '+966', label: '\uD83C\uDDF8\uD83C\uDDE6 Saudi Arabia (+966)',  maxLen: 9  },
  { code: '+20',  label: '\uD83C\uDDEA\uD83C\uDDEC Egypt (+20)',          maxLen: 10 },
  { code: '+234', label: '\uD83C\uDDF3\uD83C\uDDEC Nigeria (+234)',       maxLen: 10 },
  { code: '+254', label: '\uD83C\uDDF0\uD83C\uDDEA Kenya (+254)',         maxLen: 9  },
  { code: '+62',  label: '\uD83C\uDDEE\uD83C\uDDE9 Indonesia (+62)',      maxLen: 12 },
  { code: '+82',  label: '\uD83C\uDDF0\uD83C\uDDF7 South Korea (+82)',    maxLen: 10 },
  { code: '+52',  label: '\uD83C\uDDF2\uD83C\uDDFD Mexico (+52)',         maxLen: 10 }
];

/**
 * Initialises one phone-input widget.
 *
 * @param {string} hiddenId   - id of the hidden <input> that holds the E.164 value
 * @param {string} selectId   - id of the <select> element for the country code
 * @param {string} numberId   - id of the <input type="tel"> for the local number
 */
function initPhoneInput(hiddenId, selectId, numberId) {
  var hidden = document.getElementById(hiddenId);
  var select = document.getElementById(selectId);
  var numInput = document.getElementById(numberId);

  if (!hidden || !select || !numInput) return;

  // Populate the <select> with country options
  PHONE_COUNTRIES.forEach(function (c) {
    var opt = document.createElement('option');
    opt.value = c.code;
    opt.textContent = c.label;
    select.appendChild(opt);
  });

  // Set default country to India (+91)
  select.value = '+91';

  // If we are in edit mode the hidden field already has a value — parse it
  var existingValue = hidden.value || '';
  if (existingValue.startsWith('+')) {
    // Try to match the longest country code first (e.g. +880 before +8)
    var sorted = PHONE_COUNTRIES.slice().sort(function (a, b) {
      return b.code.length - a.code.length;
    });
    for (var i = 0; i < sorted.length; i++) {
      if (existingValue.startsWith(sorted[i].code)) {
        select.value = sorted[i].code;
        numInput.value = existingValue.slice(sorted[i].code.length);
        break;
      }
    }
  }

  // Update maxlength hint when country changes
  function updateMaxLen() {
    var country = PHONE_COUNTRIES.find(function (c) { return c.code === select.value; });
    if (country) {
      numInput.maxLength = country.maxLen;
      numInput.pattern = '[0-9]{' + country.maxLen + '}';
      numInput.title = 'Enter ' + country.maxLen + ' digits';
    }
  }

  // Combine country code + local number into the hidden field
  function combine() {
    hidden.value = select.value + numInput.value;
  }

  select.addEventListener('change', function () {
    updateMaxLen();
    combine();
  });

  numInput.addEventListener('input', function () {
    // Strip non-digits
    numInput.value = numInput.value.replace(/\D/g, '');
    combine();
  });

  // Initialise maxlength for the pre-selected country
  updateMaxLen();
  // Ensure hidden is populated from the start (edit mode)
  if (existingValue) combine();
}
